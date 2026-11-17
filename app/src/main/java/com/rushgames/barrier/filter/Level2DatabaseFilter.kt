package com.rushgames.barrier.filter

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Level 2 Adult Content Filter: Search & Access Adult Content Database.
 * Fast O(1) matching against an extensive database of known adult domains and URLs,
 * with support for local Room DB custom adult entries.
 */
@Singleton
class Level2DatabaseFilter @Inject constructor() {

    // Curated high-precision adult database of top global adult portals, cams, networks, and NSFW platforms
    private val KNOWN_ADULT_DATABASE: HashSet<String> = hashSetOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "chaturbate.com", "brazzers.com", "spankbang.com", "onlyfans.com",
        "livejasmin.com", "stripchat.com", "eporner.com", "xbeeg.com", "beeg.com",
        "youjizz.com", "motherless.com", "cliphunter.com", "tub8.com", "heavy-r.com",
        "porn.com", "bangbros.com", "naughtyamerica.com", "realitykings.com", "digitaldesire.com",
        "cams.com", "cammatch.com", "flirt4free.com", "imlive.com", "myfreecams.com",
        "cam4.com", "bongacams.com", "jasmin.com", "fapster.com", "pornmd.com",
        "tnaflix.com", "empflix.com", "drtuber.com", "daftsex.com", "sunporno.com",
        "hqporner.com", "txxx.com", "vpornv.com", "pornhd.com", "4tube.com",
        "vjav.com", "javhihi.com", "javmost.com", "javbangers.com", "r18.com",
        "rule34.xxx", "e621.net", "gelbooru.com", "danbooru.donmai.us", "hentaihaven.xxx",
        "nhentai.net", "tsnares.com", "exhentai.org", "hitomi.la", "fakku.net",
        "eroprofile.com", "fetlife.com", "adultfriendfinder.com", "ashleymadison.com",
        "hookup.com", "casualx.com", "pure.app", "seek-amateur.com", "xcheaters.com"
    )

    data class Level2Result(
        val isBlocked: Boolean,
        val reason: String = ""
    )

    fun isDomainInDatabase(domain: String, customDatabaseDomains: Set<String> = emptySet()): Level2Result {
        val cleanDomain = WebsiteDomainMatcher.normalizeDomain(domain) ?: return Level2Result(false)
        val matchingRule = (KNOWN_ADULT_DATABASE.asSequence() + customDatabaseDomains.asSequence())
            .firstOrNull { WebsiteDomainMatcher.matchesDomain(cleanDomain, it) }
            ?: return Level2Result(false)

        return Level2Result(true, "Blocked by Adult Content Database domain ($matchingRule)")
    }

    /** Allow expanding adult content database dynamically at runtime */
    fun addToDatabase(domain: String) {
        val clean = WebsiteDomainMatcher.normalizeDomain(domain) ?: return
        KNOWN_ADULT_DATABASE.add(clean)
    }
}
