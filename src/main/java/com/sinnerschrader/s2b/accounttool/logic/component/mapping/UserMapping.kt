package com.sinnerschrader.s2b.accounttool.logic.component.mapping

import com.sinnerschrader.s2b.accounttool.config.ldap.LdapConfiguration
import com.sinnerschrader.s2b.accounttool.logic.entity.User
import com.unboundid.ldap.sdk.SearchResultEntry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class UserMapping {

    @Autowired
    private lateinit var ldapConfiguration: LdapConfiguration

    fun map(entry: SearchResultEntry?): User? {
        if (entry == null) return null

        fun SearchResultEntry.str(attributeName: String) = getAttributeValue(attributeName)
        fun SearchResultEntry.int(attributeName: String) = getAttributeValueAsInteger(attributeName)
        fun SearchResultEntry.long(attributeName: String) = getAttributeValueAsLong(attributeName)

        return try {
            with(entry) {
                val birthDate =
                        str("szzBirthDate")?.let {
                            LocalDate.of(1972, it.substring(0, 1).toInt(), it.substring(2, 3).toInt())
                        } ?: parseDate(dn, false, 1972, int("szzBirthMonth"), int("szzBirthDay"))

                val entryDate = parseDate(str("szzEntryDate")) ?:
                        parseDate(dn, true, int("szzEntryYear"), int("szzEntryMonth"), int("szzEntryDay"))

                val exitDate = parseDate(str("szzExitDate")) ?:
                        parseDate(dn, true, int("szzExitYear"), int("szzExitMonth"), int("szzExitDay"))

                User(
                        dn = dn,
                        uid = str("uid"),
                        uidNumber = int("uidNumber"),
                        gidNumber = int("gidNumber"),
                        displayName = str("displayName"),
                        gecos = str("gecos"),
                        cn = str("cn"),
                        givenName = str("givenName"),
                        sn = str("sn"),
                        homeDirectory = str("homeDirectory"),
                        loginShell = str("loginShell"),
                        birthDate = birthDate,
                        sambaSID = str("sambaSID"),
                        sambaPasswordHistory = str("sambaPasswordHistory"),
                        sambaAcctFlags = str("sambaAcctFlags"),
                        mail = str("mail"),
                        szzStatus = User.State.fromString(str("szzStatus")),
                        szzMailStatus = User.State.fromString(str("szzMailStatus")),
                        sambaPwdLastSet = long("sambaPwdLastSet") ?: 0L,
                        employeeEntryDate = entryDate,
                        employeeExitDate = exitDate,
                        ou = str("ou"),
                        description = str("description"),
                        telephoneNumber = str("telephoneNumber") ?: "",
                        mobile = str("mobile") ?: "",
                        employeeNumber = str("employeeNumber"),
                        title = str("title") ?: "",
                        l = str("l"),
                        szzPublicKey = str("szzPublicKey"),
                        o = companyForDn(dn).second,
                        companyKey = companyForDn(dn).first,
                        modifiersName = str("modifiersName"),
                        modifytimestamp = str("modifytimestamp")
                )
            }
        } catch (e: Exception) {
            LOG.error("failed to map: " + entry.dn, e)
            null
        }
    }

    private fun companyForDn(dn: String) =
            try {
                with(Regex(",ou=([^,]+)").findAll(dn).last().groupValues[1]) {
                    this to (ldapConfiguration.companies[this] ?: "UNKNOWN")
                }
            } catch (e: NoSuchElementException) {
                "UNKNOWN" to "UNKNOWN"
            }


    private fun parseDate(date: String?, required: Boolean = true): LocalDate? =
            date?.let {
                try {
                    LocalDate.parse(it)
                } catch (e: Exception) {
                    if (required)
                        LOG.warn("Could not parse date [date=$date]")
                    null
                }
            }

    private fun parseDate(dn: String, required: Boolean, year: Int?, month: Int?, day: Int?): LocalDate? =
            try {
                LocalDate.of(year!!, month!!, day!!)
            } catch (e: Exception) {
                if (required)
                    LOG.warn("Could not parse date [dn={},required={},year={},month={},day={}]", dn, required, year, month, day)
                null
            }

    companion object {
        private val LOG = LoggerFactory.getLogger(UserMapping::class.java)
    }
}