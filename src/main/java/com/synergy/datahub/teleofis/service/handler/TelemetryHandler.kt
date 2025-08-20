package com.synergy.datahub.teleofis.service.handler

import com.synergy.common.device.DeviceId
import com.synergy.common.device.DeviceType
import com.synergy.datahub.data.DeviceDataListener
import com.synergy.datahub.device.api.model.DataPeriod
import com.synergy.datahub.ext.ResendTaskExt.getArchivePeriodToReceive
import com.synergy.datahub.service.data.resend.ResendRequestService
import com.synergy.datahub.service.status.MeterStatusService
import com.synergy.datahub.service.status.TransferStatusService
import com.synergy.datahub.teleofis.TeleofisDevice
import com.synergy.datahub.teleofis.api.MessageSender
import com.synergy.datahub.teleofis.mapper.InstantMapper
import com.synergy.datahub.teleofis.model.PacketType.TELEMETRY_INFORMATION_TRANSFER
import com.synergy.datahub.teleofis.model.ParameterType
import com.synergy.datahub.teleofis.model.mapper.TelemetryMessage
import com.synergy.datahub.teleofis.service.ArchiveEventProvider
import com.synergy.datahub.teleofis.service.TelemetryProvider
import com.synergy.datahub.teleofis.service.WorkModeService
import com.synergy.datahub.teleofis.service.impl.DeviceDateTimeService
import com.synergy.datahub.teleofis.service.monitoring.TeleofisMonitoringService
import com.synergy.datahub.teleofis.utils.gsmSignalLevel
import com.synergy.datahub.teleofis.utils.voltageToPercentOrNull
import com.synergy.device.model.WorkMode
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

@Component("telemetryPackageHandler")
class TelemetryPackageHandler(
    private val instantMapper: InstantMapper,
    private val changeWorkModeService: WorkModeService,
    private val deviceDateTimeService: DeviceDateTimeService,
    private val deviceDataListener: DeviceDataListener,
    private val archiveEventProvider: ArchiveEventProvider,
    private val teleofisMonitoringService: TeleofisMonitoringService,
    private val transferStatusService: TransferStatusService,
    private val meterStatusService: MeterStatusService,
) : PackageHandler<TelemetryMessage>, TelemetryProvider {
    private val log = KotlinLogging.logger {}
    private val telemetryMap = ConcurrentHashMap<ULong, Pair<Instant, TelemetryMessage>>()
    private lateinit var resendRequestService: ResendRequestService
    private val activeResendTasks = ConcurrentHashMap.newKeySet<Long>()

    override fun get(imei: ULong): TelemetryMessage? {
        return telemetryMap[imei]?.let { (updateTs, telemetry) ->
            if (ChronoUnit.MINUTES.between(updateTs, Instant.now()) > 5) {
                telemetryMap.remove(imei)
                null
            } else {
                telemetry
            }
        }
    }

    override fun handle(device: TeleofisDevice, telemetry: TelemetryMessage, sender: MessageSender) {
        notifyAboutActive(device)
        log.trace { "${device.getDeviceInfo()}Получен пакет телеметрии. Время на устройстве: ${telemetry.getCurrentTime()}. Напряжение на батарее: ${telemetry.getBatteryVoltageBeforeConnection()}" }
        deviceDateTimeService.updateDeviceDateTime(device, telemetry.getCurrentTime(), sender)
        telemetryMap[device.getImei()] = Instant.now() to telemetry
        //сменить режим работы регистратора если требуется
        changeWorkModeService.changeWorkMode(device, sender, WorkMode.Sporadic)
        instantMapper.mapToInstant(device, telemetry).onEach { (measurePoint, ddp) ->
            //отправить данные на дальнейшую обработку
            deviceDataListener.receivedData(device, ddp)
            //обновить статус переопроса для мгновенных
            resendRequestService.updateTaskStatus(measurePoint.id, ddp.type, ddp, null)
        }
        val batteryVoltage = voltageToPercentOrNull(
            checkAngGetBatteryVoltage(device, telemetry)
        ) ?: ""
        val gsmSignalLevel = gsmSignalLevel(rssi = telemetry.getGSMSignalLevel())
        notifyDeviceStatus(
            device = device,
            parameters = mapOf(
                ParameterType.GSM_SIGNAL_LEVEL.name to gsmSignalLevel.toString(),
                ParameterType.BATTERY_VOLTAGE.name to batteryVoltage.toString()
            )
        )
        //перед подтверждением на пакет авторизации важно в первую очередь отправить
        //команду на получение архивных данных за период всех зарегистрированных переопросов
        //если пришел пакет авторизации и есть активные задачи на переопрос по которым получены данные, считаем их завершенными
        runOrCompleteResendTask(device, sender)

        //отправить подтверждение на пакет телеметрии
        sender.send(device, TELEMETRY_INFORMATION_TRANSFER)
        log.trace { "${device.getDeviceInfo()}Отправлено подтверждение на пакет телеметрии" }
    }

    private fun runOrCompleteResendTask(device: TeleofisDevice, sender: MessageSender) {
        val tasks = getDeviceResendTasks(device)
        val periodToReceive = mutableListOf<DataPeriod>()
        for (task in tasks) {
            if (task.points.isNotEmpty()) {
                resendRequestService.completeTask(task)
                activeResendTasks.remove(device.getId())
            } else if (task.endDate == null) {
                periodToReceive.add(
                    getArchivePeriodToReceive(task)
                )
            }
        }
        if (periodToReceive.isNotEmpty() && activeResendTasks.add(device.getId())) {
            val period = periodToReceive
                .reduce { prev, curr ->
                    DataPeriod(
                        unit = ChronoUnit.MINUTES,
                        from = listOf(curr.from, prev.from).min(),
                        to = ZonedDateTime.now(),
                    )
                }
            log.trace { "${device.getDeviceInfo()}Запущен переопрос данных за период ${period.formattedPeriod()}" }
            archiveEventProvider.startReceiving(device, sender, period)
        }
    }

    private fun getDeviceResendTasks(device: TeleofisDevice) = device.meters
        .flatMap { meter ->
            resendRequestService.getTaskByDeviceId(
                DeviceId(transferId = device.getTransferId(), meterId = meter.id, measurePointId = meter.measurePoint.id),
            )
        }

    private fun notifyDeviceStatus(device: TeleofisDevice, parameters: Map<String, String>) {
        log.info { "${device.getDeviceInfo()}Состояние регистратора импульсов: ${parameters.entries.joinToString()}" }
        transferStatusService.notifyDeviceStatus(device.getTransferId(), DeviceType.Impulse, parameters)
    }

    private fun notifyAboutActive(device: TeleofisDevice) {
        log.warn { "${device.getDeviceInfo()}Прибор работает в спорадическом режиме" }
        transferStatusService.transferConnected(device.getTransferId(), DeviceType.Impulse)
        device.meters.forEach { meter -> meterStatusService.meterConnected(meter.id) }
    }

    private fun checkAngGetBatteryVoltage(device: TeleofisDevice, message: TelemetryMessage): Int {
        val batteryVoltage = message.getBatteryVoltageBeforeConnection()
        if (batteryVoltage <= 3000) {
            log.warn { "${device.getDeviceInfo()}Батарея регистратора разряжается. Напряжение батареии: $batteryVoltage" }
            teleofisMonitoringService.incrementLowLvlBatteryCounter(device, batteryVoltage)
        }
        return batteryVoltage;
    }

    @Autowired
    fun setResendRequestService(@org.springframework.context.annotation.Lazy resendRequestService: ResendRequestService) {
        this.resendRequestService = resendRequestService
    }

    companion object {
        fun TeleofisDevice.getDeviceInfo() = "[УПД=${getTransferId()}][IMEI=${getImei()}]"
    }
}
