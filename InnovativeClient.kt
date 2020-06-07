package ir.pegahtech.tapsell.web.acls.innovative;

import ir.pegahtech.tapsell.web.acls.innovative.models.VerificationCodeResponse
import ir.pegahtech.tapsell.web.acls.innovative.models.VerifyModel
import ir.pegahtech.tapsell.web.acls.innovative.models.VerifyResponse
import ir.pegahtech.tapsell.web.acls.innovative.models.validationRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@FeignClient(name = "innovativeClient", url = "https://irannoafarin.ir/api/v1/central-authentication")
interface InnovativeClient {

    @PostMapping("/generate", consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun verificationCode(
            @RequestHeader("Authorization") header: String,
            @RequestBody request: validationRequest
    ): VerificationCodeResponse

    @PostMapping("/verify", consumes = [MediaType.APPLICATION_JSON_UTF8_VALUE], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun verify(@RequestHeader("Authorization") header: String, @RequestBody model: VerifyModel): VerifyResponse

}
