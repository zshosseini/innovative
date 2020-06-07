package ir.pegahtech.tapsell.web.services.innovative

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ir.pegahtech.tapsell.common.acls.core.AdvertiserProfilesClient
import ir.pegahtech.tapsell.common.acls.core.UserClient
import ir.pegahtech.tapsell.web.acls.core.SearchAdUserClient
import ir.pegahtech.tapsell.web.acls.innovative.InnovativeClient
import ir.pegahtech.tapsell.web.acls.innovative.MediaAdClient
import ir.pegahtech.tapsell.web.acls.innovative.TagroClient
import ir.pegahtech.tapsell.web.acls.innovative.models.VerifyModel
import ir.pegahtech.tapsell.web.acls.innovative.models.validationRequest
import ir.pegahtech.tapsell.web.controllers.admin_v2.models.account_administration.CacheInType
import ir.pegahtech.tapsell.web.controllers.admin_v2.models.account_administration.ManualCashInRequest
import ir.pegahtech.tapsell.web.controllers.anonymous.models.InnovativeRegisterModel
import ir.pegahtech.tapsell.web.entities.InnovativeEntity
import ir.pegahtech.tapsell.web.exceptions.InnovativeException
import ir.pegahtech.tapsell.web.repositories.InnovativeRepository
import ir.pegahtech.tapsell.web.services.AccountService
import ir.pegahtech.tapsell.web.services.CampaignManipulationService
import ir.pegahtech.tapsell.web.services.TelegramService
import ir.pegahtech.tapsell.web.services.UserService
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


@Service
class InnovativeService(
        private val innovativeClient: InnovativeClient,
        private val repository: InnovativeRepository,
        private val userService: UserService,
        private val tagroClient: TagroClient,
        private val mediaAdClient: MediaAdClient,
        private val telegramService: TelegramService,
        private val campaignManipulationService: CampaignManipulationService,
        private val userClient: UserClient,
        private val accountService: AccountService,
        private val advertiserProfilesClient: AdvertiserProfilesClient,
        private val searchAdUserClient: SearchAdUserClient
) {

    private companion object {
        const val client_secret = ""
        const val client_id = ""
    }

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)
    val mapper = jacksonObjectMapper()

    fun verificationCode(innovativeCode: String) {
        val request = mapOf("innovative_code" to innovativeCode)
        val header = getHeader(request)
        val response = innovativeClient.verificationCode(header, validationRequest(innovativeCode))
        if (response.status == "ok") {
            var innovative = repository.findByInnovativeCode(innovativeCode)
                    ?: InnovativeEntity(UUID.randomUUID(), Date(), Date(), innovativeCode, 40000000L)
            innovative.token = response.data!!.toMap()["token"].toString()
            innovative.modifyDate = Date()
            repository.save(innovative)
        } else {
            logger.warn("innovative verification code error - innovativeCode : $innovativeCode - ${response.code} : ${response.message}")
            throw InnovativeException(response.message ?: "problem in receiving validation code")
        }
    }

    fun verify(innovativeCode: String, verificationCode: String): InnovativeRegisterModel {
        var innovative = repository.findByInnovativeCode(innovativeCode)
                ?: throw InnovativeException("requested innovativeCode not found")

        val request = VerifyModel(innovative.innovativeCode, innovative.token, verificationCode)
        val header = getHeader(request.toMap())
        val response = innovativeClient.verify(header, request)

        if (response.status == "ok") {
            innovative.requestId = response.request_id
            innovative.userType = response.user_type
            fillInnovativeData(innovative, response.data.toMap())

            repository.save(innovative)

            var registerModel = InnovativeRegisterModel(
                    innovative.requestId!!,
                    innovative.innovativeCode,
                    innovative.packages,
                    innovative.remainBudget,
                    innovative.ceoEmail ?: "",
                    innovative.name ?: "",
                    innovative.familyName ?: "",
                    "",
                    innovative.cellPhoneNumber ?: "",
                    innovative.address ?: "",
                    innovative.postalCode ?: "",
                    innovative.city ?: "",
                    innovative.companyName ?: ""
            )

            if (innovative.tapsellUserId != null) registerModel.tapsellLoginUrl = userService.getLoginUrl(innovative.tapsellUserId)
//            if (innovative.mediaAdUserId != null ) registerModel.mediaAdLoginUrl = mediaAdClient.loginUrl("", innovative.mediaAdUserId!!)
            if (innovative.mediaAdUserId != null) registerModel.mediaAdLoginUrl = "mediaAdLoginUrl"
//            if (innovative.tagroUserId != null ) registerModel.tagroLoginUrl = tagroClient.loginUrl("", innovative.tagroUserId!!)
            if (innovative.tagroUserId != null) registerModel.tagroLoginUrl = "tagroLoginUrl"

            return registerModel
        } else {
            logger.warn("innovative verify error - innovativeCode : $innovativeCode - ${response.code} : ${response.message}")
            throw InnovativeException(response.message ?: "problem in verifying validation code")
        }

    }

    

    private fun fillInnovativeData(innovative: InnovativeEntity, data: Map<String, Any>) {
        innovative.address = data["Address"]?.toMap()?.get("value")?.toString()
        innovative.cellPhoneNumber = data["UserCellPhoneNumber"]?.toMap()?.get("value")?.toString()
        innovative.city = data["City"]?.toMap()?.get("value")?.toString()
        innovative.familyName = data["FamilyName"]?.toMap()?.get("value")?.toString()
        innovative.growthStage = data["GrowthStage"]?.toMap()?.get("value")?.toString()
        innovative.name = data["Name"]?.toMap()?.get("value")?.toString()
        innovative.postalCode = data["PostalCode"]?.toMap()?.get("value")?.toString()
        innovative.siteAddress = data["SiteAddress"]?.toMap()?.get("value")?.toString()
        innovative.startUpTeamName = data["StartUpTeamName"]?.toMap()?.get("value")?.toString()
        innovative.state = data["State"]?.toMap()?.get("value")?.toString()
        innovative.ceoEmail = data["CeoEmail"]?.toMap()?.get("value")?.toString()
        innovative.ceoName = data["CeoName"]?.toMap()?.get("value")?.toString()
        innovative.ceoFamilyName = data["CeoFamilyName"]?.toMap()?.get("value")?.toString()
        innovative.ceoNationalCode = data["CeoNationalCode"]?.toMap()?.get("value")?.toString()
        innovative.ceoMobilePhone = data["CeoMobilePhone"]?.toMap()?.get("value")?.toString()
        innovative.economicCode = data["EconomicCode"]?.toMap()?.get("value")?.toString()
        innovative.nationalIdCompany = data["NationalIdCompany"]?.toMap()?.get("value")?.toString()
        innovative.companyName = data["CompanyName"]?.toMap()?.get("value")?.toString()
        innovative.nationalCode = data["NationalCode"]?.toMap()?.get("value")?.toString()
        innovative.fatherName = data["FatherName"]?.toMap()?.get("value")?.toString()
        innovative.birthday = data["Birthday"]?.toMap()?.get("value")?.toString()
    }

    fun getHeader(body: Map<String, Any>): String {
        val signature = generateSignature(client_secret, generateQueryString(body))
        return "HMAC $client_id:$signature"
    }

    private fun generateQueryString(body: Map<String, Any>): String {
        return body.toSortedMap().entries.stream()
                .map { p -> p.key.toString() + "=" + p.value }
                .reduce { p1, p2 -> "$p1&$p2" }
                .orElse("")
    }

    fun generateSignature(key: String, data: String): String? {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(charset("UTF-8")), "HmacSHA256")
        sha256Hmac.init(secretKey)
        return Hex.encodeHexString(sha256Hmac.doFinal(data.toByteArray(charset("UTF-8"))))
    }

    fun <T> T.toMap(): Map<String, Any> {
        return convert()
    }

    private inline fun <T, reified R> T.convert(): R {
        val json = mapper.writeValueAsString(this)
        return mapper.readValue(json, R::class.java)
    }

}
