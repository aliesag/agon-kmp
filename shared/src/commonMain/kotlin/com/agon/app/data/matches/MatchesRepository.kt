package com.agon.app.data.matches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.agon.app.Http

object MatchesRepository {

    private const val YALLA_SHOOT_URL = "https://yalla.moda/"
    private const val YALLA_YESTERDAY_URL = "https://yalla.moda/matches-yesterday/"
    private const val YALLA_TOMORROW_URL = "https://yalla.moda/matches-tomorrow1/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Curated complete match fixtures representing Yalla Shoot data
     * (Today, Yesterday, and Tomorrow) with 100% offline fallback and fast loading.
     */
    fun getInitialMatches(): List<MatchItem> {
        return listOf(
            // ==========================================
            // --- TODAY MATCHES (مباريات اليوم) ---
            // ==========================================
            MatchItem(
                id = "match_barca_racing",
                team1Name = "برشلونة",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/FC-Barcelona.png",
                team2Name = "راسينج سانتاندير",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/137.png",
                score1 = "7",
                score2 = "2",
                matchTime = "22:30",
                status = MatchStatus.FINISHED,
                tournament = "إسبانيا, الدوري الإسباني",
                channel = "beIN 2HD",
                commentator = "عامر الخوذيري",
                streamPageUrl = "https://yalla.moda/matches/fc-barcelona-vs-racing-santander/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD"),
                    MatchServer("s3", "سيرفر 3 (احتياطي)", "https://mm.yallagoo.xyz/albaplayer/sport-2/", "SD")
                ),
                day = MatchDay.TODAY,
                liveMinute = null
            ),
            MatchItem(
                id = "match_inter_miami",
                team1Name = "انتر ميامي",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/09/54729.png",
                team2Name = "كروز أزول",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/09/1261.png",
                score1 = "0",
                score2 = "0",
                matchTime = "03:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "شمال امريكا, Campeones Cup - النهائي",
                channel = "Apple TV",
                commentator = "English",
                streamPageUrl = "https://yalla.moda/matches/inter-miami-cf-vs-cd-cruz-azul/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TODAY
            ),
            MatchItem(
                id = "match_city_norwich",
                team1Name = "مانشستر سيتي",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Manchester-City.png",
                team2Name = "نورويتش سيتي",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/02/16.png",
                score1 = "0",
                score2 = "0",
                matchTime = "21:30",
                status = MatchStatus.NOT_STARTED,
                tournament = "إنجلترا, كأس الكاراباو - الدور 3",
                channel = "Bein Sports HD2",
                commentator = "حفيظ دراجي",
                streamPageUrl = "https://yalla.moda/matches/manchester-city-vs-norwich-city/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TODAY
            ),
            MatchItem(
                id = "match_betis_getafe",
                team1Name = "ريال بيتيس",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Real-Betis-Balompie.png",
                team2Name = "خيتافي",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/Getafe-CF.png",
                score1 = "0",
                score2 = "0",
                matchTime = "20:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "إسبانيا, الدوري الإسباني",
                channel = "beIN Sports 6 HD",
                commentator = "أحمد البلوشي",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TODAY
            ),
            MatchItem(
                id = "match_juventus_nijmegen",
                team1Name = "يوفنتوس",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Juventus-FC.png",
                team2Name = "نيميخين",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/700.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "أوروبا, الدوري الأوروبي",
                channel = "beIN SPORTS HD 3",
                commentator = "علي محمد علي",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TODAY
            ),
            MatchItem(
                id = "match_zed_elqanah",
                team1Name = "زد",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/01/Zed-FC.png",
                team2Name = "القناة",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/08/12252.png",
                score1 = "0",
                score2 = "0",
                matchTime = "17:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "مصر, الدوري المصري",
                channel = "OnTime Sports 2",
                commentator = "حاتم بطيشة",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.TODAY
            ),
            MatchItem(
                id = "match_palace_poznan",
                team1Name = "كريستال بالاس",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Crystal-Palace.png",
                team2Name = "ليخ بوزنان",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/02/2211.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "أوروبا, الدوري الأوروبي",
                channel = "beIN SPORTS HD 4",
                commentator = "خالد الحدي",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.TODAY
            ),
            MatchItem(
                id = "match_besiktas_marseille",
                team1Name = "بشكتاش",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/09/947.png",
                team2Name = "أولمبيك مارسيليا",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/Olympique-Marseille.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "أوروبا, الدوري الأوروبي",
                channel = "beIN SPORTS HD 7",
                commentator = "جواد بدة",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.TODAY
            ),

            // ==========================================
            // --- YESTERDAY MATCHES (مباريات الأمس) ---
            // ==========================================
            MatchItem(
                id = "match_y_elgouna_petrojet",
                team1Name = "الجونة",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/El-Gouna-FC.png",
                team2Name = "منتخب السويس بتروجت",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/08/8302.png",
                score1 = "1",
                score2 = "2",
                matchTime = "17:00",
                status = MatchStatus.FINISHED,
                tournament = "مصر, الدوري المصري",
                channel = "أون سبورت بلس",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/matches/el-gouna-fc-vs-petrojet/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_nahda_taawoun",
                team1Name = "النهضة",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/02/12184.png",
                team2Name = "التعاون",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/04/Al-Taawoun-FC.png",
                score1 = "1",
                score2 = "1",
                matchTime = "19:00",
                status = MatchStatus.FINISHED,
                tournament = "آسيا, دوري أبطال آسيا 2",
                channel = "beIN 4HD",
                commentator = "حسن العيدروس",
                streamPageUrl = "https://yalla.moda/matches/al-nahda-club-vs-al-taawoun-fc/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_wahda_kuwait",
                team1Name = "الوحدة",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/04/Al-Wahda-FC.png",
                team2Name = "الكويت",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/09/Kuwait-SC.png",
                score1 = "3",
                score2 = "2",
                matchTime = "19:00",
                status = MatchStatus.FINISHED,
                tournament = "آسيا, دوري أبطال آسيا 2",
                channel = "beIN 3HD",
                commentator = "عبدالله الغامدي",
                streamPageUrl = "https://yalla.moda/matches/al-wahda-fc-vs-kuwait-sc/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_omonia_celta",
                team1Name = "أومونيا نيقوسيا",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/02/2262.png",
                team2Name = "سيلتا فيغو",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/Celta-de-Vigo-1.png",
                score1 = "1",
                score2 = "0",
                matchTime = "19:45",
                status = MatchStatus.FINISHED,
                tournament = "أوروبا, الدوري الأوروبي",
                channel = "beIN 1HD",
                commentator = "نوفل باشي",
                streamPageUrl = "https://yalla.moda/matches/omonia-nicosia-vs-celta-de-vigo/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_atletico_osasuna",
                team1Name = "أتلتيكو مدريد",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Atletico-de-Madrid.png",
                team2Name = "أوساسونا",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/CA-Osasuna.png",
                score1 = "4",
                score2 = "0",
                matchTime = "20:00",
                status = MatchStatus.FINISHED,
                tournament = "إسبانيا, الدوري الإسباني",
                channel = "Bein Sports HD2",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/matches/atl%c3%a9tico-de-madrid-vs-ca-osasuna/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_mahalla_zamalek",
                team1Name = "غزل المحلة",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Ghazl-El-Mahalla.png",
                team2Name = "الزمالك",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/Zamalek-SC.png",
                score1 = "0",
                score2 = "1",
                matchTime = "20:00",
                status = MatchStatus.FINISHED,
                tournament = "مصر, الدوري المصري",
                channel = "On Sport",
                commentator = "بلال علام",
                streamPageUrl = "https://yalla.moda/matches/ghazl-el-mahalla-vs-zamalek-sc/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_milan_benfica",
                team1Name = "ميلان",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/04/AC-Milan.png",
                team2Name = "بنفيكا",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/888.png",
                score1 = "0",
                score2 = "2",
                matchTime = "22:00",
                status = MatchStatus.FINISHED,
                tournament = "أوروبا, الدوري الأوروبي",
                channel = "beIN 1HD",
                commentator = "خليل البلوشي",
                streamPageUrl = "https://yalla.moda/matches/ac-milan-vs-sl-benfica/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_manutd_brighton",
                team1Name = "مانشستر يونايتد",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Manchester-United.png",
                team2Name = "برايتون",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/Brighton-Hove-Albion.png",
                score1 = "2",
                score2 = "3",
                matchTime = "22:00",
                status = MatchStatus.FINISHED,
                tournament = "إنجلترا, كأس الكاراباو - الدور 3",
                channel = "beIN 4HD",
                commentator = "محمد بركات",
                streamPageUrl = "https://yalla.moda/matches/manchester-united-vs-brighton-hove-albion/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_barca_racing",
                team1Name = "برشلونة",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/FC-Barcelona.png",
                team2Name = "راسينج سانتاندير",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/137.png",
                score1 = "7",
                score2 = "2",
                matchTime = "22:30",
                status = MatchStatus.FINISHED,
                tournament = "إسبانيا, الدوري الإسباني",
                channel = "beIN 2HD",
                commentator = "عامر الخوذيري",
                streamPageUrl = "https://yalla.moda/matches/fc-barcelona-vs-racing-santander/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),
            MatchItem(
                id = "match_y_levante_bilbao",
                team1Name = "ليفانتي",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Levante-UD.png",
                team2Name = "أتلتيك بلباو",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/Athletic-Bilbao.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:30",
                status = MatchStatus.FINISHED,
                tournament = "إسبانيا, الدوري الإسباني",
                channel = "beIN SPORTS HD 9",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/matches/levante-ud-vs-athletic-bilbao/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.YESTERDAY
            ),

            // ==========================================
            // --- TOMORROW MATCHES (مباريات الغد) ---
            // ==========================================
            MatchItem(
                id = "match_t_bayern_union",
                team1Name = "بايرن ميونخ",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Bayern-Munich.png",
                team2Name = "يونيون برلين",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/1.FC-Union-Berlin.png",
                score1 = "0",
                score2 = "0",
                matchTime = "21:30",
                status = MatchStatus.NOT_STARTED,
                tournament = "ألمانيا, الدوري الألماني",
                channel = "MBC Action",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TOMORROW
            ),
            MatchItem(
                id = "match_t_monza_sassuolo",
                team1Name = "مونزا",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/08/293.png",
                team2Name = "ساسولو",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/US-Sassuolo.png",
                score1 = "0",
                score2 = "0",
                matchTime = "21:45",
                status = MatchStatus.NOT_STARTED,
                tournament = "إيطاليا, الدوري الإيطالي",
                channel = "Starzplay",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TOMORROW
            ),
            MatchItem(
                id = "match_t_monaco_lens",
                team1Name = "موناكو",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/AS-Monaco.png",
                team2Name = "لانس",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/01/RC-Lens.png",
                score1 = "0",
                score2 = "0",
                matchTime = "21:45",
                status = MatchStatus.NOT_STARTED,
                tournament = "فرنسا, الدوري الفرنسي",
                channel = "beIN SPORTS HD 4",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TOMORROW
            ),
            MatchItem(
                id = "match_t_brentford_chelsea",
                team1Name = "برينتفورد",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/Brentford-FC.png",
                team2Name = "تشيلسي",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/Chelsea-FC.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "إنجلترا, الدوري الإنجليزي",
                channel = "beIN SPORTS HD 1",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TOMORROW
            ),
            MatchItem(
                id = "match_t_bristol_watford",
                team1Name = "بريستول سيتي",
                team1Logo = "https://yalla.moda/wp-content/uploads/2026/02/6.png",
                team2Name = "واتفورد",
                team2Logo = "https://yalla.moda/wp-content/uploads/2026/09/4.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "إنجلترا, تشامبيونشيب",
                channel = "غير معروف",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                day = MatchDay.TOMORROW
            ),
            MatchItem(
                id = "match_t_espanyol_elche",
                team1Name = "إسبانيول",
                team1Logo = "https://yalla.moda/wp-content/uploads/2025/12/RCD-Espanyol-Barcelona.png",
                team2Name = "إلتشي",
                team2Logo = "https://yalla.moda/wp-content/uploads/2025/12/Elche-CF.png",
                score1 = "0",
                score2 = "0",
                matchTime = "22:00",
                status = MatchStatus.NOT_STARTED,
                tournament = "إسبانيا, الدوري الإسباني",
                channel = "beIN Sports 3 HD",
                commentator = "غير معروف",
                streamPageUrl = "https://yalla.moda/",
                embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/",
                servers = listOf(
                    MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                    MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD")
                ),
                day = MatchDay.TOMORROW
            )
        )
    }

    /**
     * Curated sports news with full article text from Yalla Shoot.
     */
    fun getInitialNews(): List<SportsNewsItem> {
        return listOf(
            SportsNewsItem(
                id = "news_alonso_arsenal",
                title = "\"إنها رحلة طويلة للغاية\" – لم يُحبط ألونسو من الهزيمة أمام أرسنال",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/09/Alonso.webp",
                url = "https://yalla.moda/إنها-رحلة-طويلة-للغاية-لم-يُحبط-ألون/",
                category = "أخبار الرياضة",
                content = """
أصر تشابي ألونسو على أن فريقه تشيلسي سيتعافى من هزيمته الضئيلة أمام أرسنال، قائلاً إن فريقه مستعد "للرحلة الطويلة" التي تنتظرهم.

تلقى ألونسو أول هزيمة له في الدوري الإنجليزي الممتاز منذ توليه تدريب تشيلسي في ملعب الإمارات، حيث قلب أرسنال تأخره بهدف إلى فوز 2-1.

بدأ تشيلسي بداية خاطفة عندما سدد مورغان روجرز كرة طائرة تجاوزت ديفيد رايا بعد 77 ثانية فقط، وهو أسرع هدف استقبله أرسنال منذ أغسطس 2023.

لكن بعد إلغاء هدف ريكاردو كالافيوري، رد رجال ميكيل أرتيتا عن طريق كاي هافرتز في الدقيقة 25، حيث سدد كرة مرت من بين يدي إيميليانو مارتينيز.

اكتملت عودة أرسنال بعد خمس دقائق من بداية الشوط الثاني عندما استلم مارتن أوديجارد تمريرة كريستوس تزوليس قبل أن يسدد بقوة في شباك تشيلسي.

وأضاف ألونسو: "الأمر مؤلم، إنه مؤلم لأن الهزيمة يجب أن تكون مؤلمة. لقد كانت مباراة شديدة التوتر مع لحظات مختلفة لكل شيء. سنواصل، إنها المباراة الثالثة، والرحلة طويلة جداً."
                """.trimIndent(),
                date = "7 سبتمبر 2026"
            ),
            SportsNewsItem(
                id = "news_gary_neville",
                title = "غاري نيفيل: دفاع تشيلسي في حالة فوضى، وقد يحتاجون إلى المزيد من التعاقدات.",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/09/Gary-Neville-Chelseas-defense-in-chaos-they-may-need-more-signings-350x147.webp",
                url = "https://yalla.moda/%d8%ba%d8%a7%d8%b1%d9%8a-%d9%86%d9%8a%d9%81%d9%8a%d9%84-%d8%af%d9%81%d8%a7%d8%b9-%d8%aa%d8%b4%d9%8a%d9%84%d8%b3%d9%8a-%d9%81%d9%8a-%d8%ad%d8%a7%d9%84%d8%a9-%d9%81%d9%88%d8%b6%d9%89%d8%8c-%d9%88%d9%82/",
                category = "الدوري الإنجليزي",
                content = """
أكد أسطورة مانشستر يونايتد والمحلل الرياضي غاري نيفيل أن الخط الخلفي لفريق تشيلسي يعاني من عدم التوازن والارتباك في المباريات الكبرى.

وأشار نيفيل إلى أن الفريق يمتلك جودة هجومية عالية لكنه بحاجة إلى قائد في قلب الدفاع ينظم الخطوط ويمنع استقبال الأهداف السهلة من الكرات الثابتة والمرتدات.
                """.trimIndent(),
                date = "اليوم"
            ),
            SportsNewsItem(
                id = "news_arteta_arsenal",
                title = "أرتيتا: بإمكان آرسنال تقديم أداء أفضل من الموسم الماضي وتحدي الصدارة",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/09/Arteta-5-350x197.webp",
                url = "https://yalla.moda/%d8%a3%d8%b1%d8%aa%d9%8a%d8%aa%d8%a7-%d8%a8%d8%a5%d9%85%d9%83%d8%a7%d9%86-%d8%a2%d8%b1%d8%b3%d9%86%d8%a7%d9%84-%d8%aa%d9%82%d8%af%d9%8a%d9%85-%d8%a3%d8%af%d8%a7%d8%a1-%d8%a3%d9%81%d8%b6%d9%84-%d9%85/",
                category = "الدوري الإنجليزي",
                content = """
عبر المدير الفني لنادي آرسنال ميكيل أرتيتا عن ثقته الكاملة في تشكيلة فريقه وقدرتهم على التتويج بلقب الدوري الإنجليزي هذا الموسم.

وقال أرتيتا: "تعلمنا الكثير من التجارب السابقة ونمتلك الآن عمقاً أكبر في كل مركز، والروح الانتصارية لدى اللاعبين هي السلاح الأهم."
                """.trimIndent(),
                date = "اليوم"
            ),
            SportsNewsItem(
                id = "news_flick_barca",
                title = "لا يزال فليك يسعى للكمال رغم فوز برشلونة الساحق على فالنسيا",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/09/Flick-350x197.webp",
                url = "https://yalla.moda/%d9%84%d8%a7-%d9%8a%d8%b2%d8%a7%d9%84-%d9%81%d9%84%d9%8a%d9%83-%d9%8a%d8%b3%d8%b9%d9%89-%d9%84%d9%84%d9%83%d9%85%d8%a7%d9%84-%d8%b1%d8%ba%d9%85-%d9%81%d9%88%d8%b2-%d8%a8%d8%b1%d8%b4%d9%84%d9%88%d9%86/",
                category = "الدوري الإسباني",
                content = """
رغم العرض الهجومي الكاسح والأهداف الغزيرة التي سجلها برشلونة، شدد المدرب الألماني هانسي فليك على أهمية الحفاظ على التركيز الدفاعي طوال الـ 90 دقيقة وتفادي الأخطاء الفردية.
                """.trimIndent(),
                date = "اليوم"
            ),
            SportsNewsItem(
                id = "news_sancho_dortmund",
                title = "أخبار الانتقالات: جادون سانشو مرشح للعودة المذهلة إلى دورتموند",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/07/Paper-Talk-350x197.webp",
                url = "https://yalla.moda/%d8%a3%d8%ae%d8%a8%d8%a7%d8%b1-%d8%a7%d9%84%d8%a7%d9%86%d8%aa%d9%82%d8%a7%d9%84%d8%a7%d8%aa-%d8%ac%d8%a7%d8%af%d9%88%d9%86-%d8%b3%d8%a7%d9%86%d8%b4%d9%88-%d9%85%d8%b1%d8%b4%d8%ad-%d9%84%d9%84%d8%b9/",
                category = "سوق الانتقالات",
                content = """
تشير التقارير الصحفية إلى محادثات متقدمة لإعادة الجناح الدولي الإنجليزي جادون سانشو إلى صفوف بوروسيا دورتموند الألماني حيث قدم أفضل فترات مسيرته الكروية.
                """.trimIndent(),
                date = "اليوم"
            ),
            SportsNewsItem(
                id = "news_wijnaldum_ittihad",
                title = "رسميًا: انضم فينالدوم، البالغ من العمر 35 عامًا، إلى الاتحاد في صفقة انتقال حر",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/09/Wijnaldum-350x438.webp",
                url = "https://yalla.moda/%d8%b1%d8%b3%d9%85%d9%8a%d9%8b%d8%a7-%d8%a7%d9%86%d8%b6%d9%85-%d9%81%d9%8a%d9%86%d8%a7%d9%84%d8%af%d9%88%d9%85%d8%8c-%d8%a7%d9%84%d8%a8%d8%a7%d9%84%d8%ba-%d9%85%d9%86-%d8%a7%d9%84%d8%b9%d9%85%d8%b1-3/",
                category = "دوري روشن السعودي",
                content = """
أعلن نادي الاتحاد السعودي رسميًا عن تعاقده مع لاعب الوسط الهولندي جورجينيو فينالدوم لتدعيم صفوف الفريق في المنافسات المحلية والقارية.
                """.trimIndent(),
                date = "اليوم"
            ),
            SportsNewsItem(
                id = "news_maresca_haaland",
                title = "ماريسكا: نحن بحاجة ماسة لحماية هالاند؛ سيمينيو يستطيع اللعب كمهاجم صريح",
                imageUrl = "https://yalla.moda/wp-content/uploads/2026/09/Maresca-1-350x253.webp",
                url = "https://yalla.moda/%d9%85%d8%a7%d8%b1%d9%8a%d8%b3%d9%83%d8%a7-%d9%86%d8%ad%d9%86-%d8%a8%d8%ad%d8%a7%d8%ac%d8%a9-%d9%85%d8%a7%d8%b3%d8%a9-%d9%84%d8%ad%d9%85%d8%a7%d9%8a%d8%a9-%d9%87%d8%a7%d9%84%d8%a7%d9%86%d8%af%d8%9b/",
                category = "الدوري الإنجليزي",
                content = """
تحدث المدرب إينزو ماريسكا عن ضرورة توفير الحماية للاعبين المؤثرين من الإرهاق البدني والتدخلات العنيفة، مشيداً بالمرونة التكتيكية التي يمتلكها فريقه.
                """.trimIndent(),
                date = "اليوم"
            )
        )
    }

    /**
     * Live scraping with scraping from Today, Yesterday, and Tomorrow endpoints.
     */
    suspend fun fetchLiveMatches(): List<MatchItem> = withContext(Dispatchers.Default) {
        val allScraped = mutableListOf<MatchItem>()
        val initial = getInitialMatches()

        // 1. Scrape Today
        try {
            val todayHtml = Http.get(YALLA_SHOOT_URL, mapOf("User-Agent" to USER_AGENT))
            val todayDoc = Ksoup.parse(todayHtml)
            val todayMatches = parseMatchesFromDoc(todayDoc, MatchDay.TODAY)
            if (todayMatches.isNotEmpty()) {
                allScraped.addAll(todayMatches)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Scrape Yesterday
        try {
            val yesterdayHtml = Http.get(YALLA_YESTERDAY_URL, mapOf("User-Agent" to USER_AGENT))
            val yesterdayDoc = Ksoup.parse(yesterdayHtml)
            val yesterdayMatches = parseMatchesFromDoc(yesterdayDoc, MatchDay.YESTERDAY)
            if (yesterdayMatches.isNotEmpty()) {
                allScraped.addAll(yesterdayMatches)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Scrape Tomorrow
        try {
            val tomorrowHtml = Http.get(YALLA_TOMORROW_URL, mapOf("User-Agent" to USER_AGENT))
            val tomorrowDoc = Ksoup.parse(tomorrowHtml)
            val tomorrowMatches = parseMatchesFromDoc(tomorrowDoc, MatchDay.TOMORROW)
            if (tomorrowMatches.isNotEmpty()) {
                allScraped.addAll(tomorrowMatches)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (allScraped.isNotEmpty()) {
            // Fill missing days from curated baseline if any day had no network response
            for (m in initial) {
                if (allScraped.none { it.team1Name == m.team1Name && it.day == m.day }) {
                    allScraped.add(m)
                }
            }
            allScraped
        } else {
            initial
        }
    }

    private fun parseMatchesFromDoc(doc: Document, matchDay: MatchDay): List<MatchItem> {
        val matchElements = doc.select(".AY_Match")
        val result = mutableListOf<MatchItem>()

        for (elem in matchElements) {
            try {
                val team1Name = elem.select(".TM1 .TM_Name").text().trim().ifEmpty { "فريق 1" }
                val team1Logo = elem.select(".TM1 .TM_Logo img").attr("data-src").ifEmpty {
                    elem.select(".TM1 .TM_Logo img").attr("src")
                }

                val team2Name = elem.select(".TM2 .TM_Name").text().trim().ifEmpty { "فريق 2" }
                val team2Logo = elem.select(".TM2 .TM_Logo img").attr("data-src").ifEmpty {
                    elem.select(".TM2 .TM_Logo img").attr("src")
                }

                val time = elem.select(".MT_Time").text().trim().ifEmpty { "20:00" }
                val goals = elem.select(".MT_Result .RS-goals")
                val score1 = if (goals.size >= 2) goals[0].text().trim() else "0"
                val score2 = if (goals.size >= 2) goals[1].text().trim() else "0"

                val statText = elem.select(".MT_Stat").text().trim()
                val status = when {
                    elem.hasClass("live") || statText.contains("جارية") || statText.contains("مباشر") -> MatchStatus.LIVE
                    elem.hasClass("finished") || statText.contains("انتهت") || matchDay == MatchDay.YESTERDAY -> MatchStatus.FINISHED
                    else -> MatchStatus.NOT_STARTED
                }

                val infoLis = elem.select(".MT_Info li span")
                val channel = if (infoLis.size >= 1) infoLis[0].text().trim() else "beIN Sports"
                val commentator = if (infoLis.size >= 2) infoLis[1].text().trim() else "غير معروف"
                val tournament = if (infoLis.size >= 3) infoLis[2].text().trim() else "بطولة رسمية"

                val matchLink = elem.select("a[href]").attr("href")
                val embedUrl = "https://mm.yallagoo.xyz/albaplayer/sport-2/"

                val id = "match_" + matchDay.name + "_" + (team1Name + "_" + team2Name).hashCode()

                result.add(
                    MatchItem(
                        id = id,
                        team1Name = team1Name,
                        team1Logo = team1Logo,
                        team2Name = team2Name,
                        team2Logo = team2Logo,
                        score1 = score1,
                        score2 = score2,
                        matchTime = time,
                        status = status,
                        tournament = tournament,
                        channel = channel,
                        commentator = commentator,
                        streamPageUrl = matchLink,
                        embedUrl = embedUrl,
                        servers = listOf(
                            MatchServer("s1", "سيرفر 1", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=1", "FHD"),
                            MatchServer("s2", "سيرفر 2", "https://mm.yallagoo.xyz/albaplayer/sport-2/?serv=2", "HD"),
                            MatchServer("s3", "سيرفر 3 (متعدد الجودات)", "https://mm.yallagoo.xyz/albaplayer/sport-2/", "SD")
                        ),
                        day = matchDay,
                        liveMinute = if (status == MatchStatus.LIVE) "مباشر الآن" else null
                    )
                )
            } catch (e: Exception) {
                // Continue with next
            }
        }
        return result
    }

    /**
     * Attempts live scraping of sports news.
     */
    suspend fun fetchLiveNews(): List<SportsNewsItem> = withContext(Dispatchers.Default) {
        try {
            val html = Http.get(YALLA_SHOOT_URL, mapOf("User-Agent" to USER_AGENT))
            val doc = Ksoup.parse(html)

            val items = doc.select(".AlbaGridPosts .gr-item")
            if (items.isEmpty()) return@withContext getInitialNews()

            val parsedNews = mutableListOf<SportsNewsItem>()
            val initialNews = getInitialNews()

            for (item in items) {
                val title = item.select(".gr-title").text().trim()
                if (title.isEmpty()) continue
                val img = item.select(".gr-img").attr("data-src").ifEmpty {
                    item.select(".gr-img").attr("src")
                }
                val link = item.select("a.gr-inner").attr("href")
                val matchedInitial = initialNews.find { it.title.contains(title.take(15)) || title.contains(it.title.take(15)) }

                parsedNews.add(
                    SportsNewsItem(
                        id = "news_" + kotlin.random.Random.nextLong().toString(),
                        title = title,
                        imageUrl = img,
                        url = link,
                        category = "أخبار الرياضة",
                        content = matchedInitial?.content?.ifEmpty { "تابع تفاصيل الخبر الرياضي والتحليلات الحصرية لحظة بلحظة عبر تطبيق حكايتنا." }
                            ?: "تابع تفاصيل الخبر الرياضي والتحليلات الحصرية لحظة بلحظة عبر تطبيق حكايتنا.",
                        date = "اليوم"
                    )
                )
            }
            if (parsedNews.isNotEmpty()) parsedNews else getInitialNews()
        } catch (e: Exception) {
            getInitialNews()
        }
    }
}
