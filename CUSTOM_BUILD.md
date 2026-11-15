# Ahmedabad custom launcher

Based on Free My TV v1.3.0, commit 900d5c86b32dfc5557c312a17f9604c63583abdc.
Local Android version: 1.3.0.2 (versionCode 6).

## Resulting behavior

- Recognizes the installed SonyLIV app (`com.sonyliv`) as a recommendation source. Exact TMDB provider names `Sony Liv` and `Sonyliv` are supported; subscriptions sold through Amazon Channels remain excluded.
- Unions TMDB's movie and TV provider catalogs so TV-only services are not omitted.
- The private local config enables `prefer_indian=true` and fixes streaming availability to India (`tmdb_region=IN`).
- Indian productions across all languages are preferred. A three-card tab aims for two Indian titles followed by one international title. Six cards aim for four Indian and two international titles. Scarce catalogs fall back to other eligible titles.
- Freshness now takes priority within each country group: movies released within 90 days, shows with an episode aired within 30 days. Available fresh titles are selected before older fallback titles even when the fresh pool cannot fill the whole row. These are TMDB release/episode dates, not dates a title was added to a particular streaming app.
- Local candidates come from TMDB discovery with `with_origin_country=IN`; details also check origin/production countries. Eligibility still requires India subscription availability and a launchable installed app enabled in Picks from.
- A maximum of 24 candidates is retained, with the existing background refresh and image cache. Hourly rotation retains franchise deduplication and at most one Plex title per tab.
- All Indian-content logic is conditional on the build option and India region. The ordinary selection order remains available with `prefer_indian=false`.
- Local defaults: Ahmedabad, Celsius, dark theme. No TMDB token is embedded in the APK or stored in this source tree.

## Live & Upcoming (Android TV)

- Added a fourth home tab using the public TheSportsDB v1 schedule and event-detail endpoints. It only accepts India listings for Sony LIV channel 1168 and India Hotstar channel 582, mapped to the installed SonyLIV and JioHotstar apps. TV-only channels are not used as proof of streaming availability.
- This free feed has limited coverage (currently one upcoming listing per queried app). The feature does not claim to list every match, sport, or streaming app.
- Start times are parsed as UTC and displayed in IST. Only the next seven days are shown. Finished, cancelled, postponed and invalid events are hidden.
- A Live badge requires an explicit in-progress status checked within 15 minutes. Passing the scheduled start time alone never creates a Live badge; old live status expires. Schedule data expires after six hours without a successful check.
- The schedule is cached locally. Home requests a refresh at most every 15 minutes while visible, or when eligible installed/selected apps change. There is no background job or foreground service. An in-flight request may finish after leaving Home.
- Cards use cached text layouts and gradients, with no sports-image downloads, trailers, live video, or scores polling. The source is credited on the tab.
- Selecting a sports card opens its associated installed app. Direct-to-match playback and subscription entitlement are not verified.

## Verification completed

- Android build compiled against android-34 using installed build-tools 35.0.0 and JDK 17; APK signature verified.
- `tools/check.sh android` passed, including the plain JVM recommendation-mix tests.
- Tests cover 48-hour rotation, 2:1 and 4:2 ratios, catalog scarcity, franchise duplicates, one-Plex limit, ordinary rotation, and divisibility cases that could otherwise starve candidates.
- Translation catalog check and `git diff --check` passed.
- User-approved read-only TMDB tests verified direct Sony Liv provider 237 for India. Indian-origin movie and TV samples had subscription availability and artwork. Amazon-channel variants were distinct and excluded.
- Installed on the TV with user approval. Android reports version 1.3.0.1 / versionCode 5, with Free My TV resolving as the Home activity.
- Verified on the Shows tab: Anupamaa and F.I.R. appeared before the international title The Greatest, with artwork loaded. F.I.R.'s provider label was SonyLIV.
- Selecting the F.I.R. recommendation opened `com.sonyliv/.ui.splash.SplashActivity`; Home returned to `io.freemytv/.HomeActivity`. This verifies app launch, not direct-to-episode playback or subscription entitlement.
- Restored the six-app shelf: YouTube, Prime Video, JioHotstar, TiviMate, SonyLIV, Play Store. Restored the supplied token through the TV's one-time setup page; it is not stored in the project or APK.
- Restored all prior debloat settings after temporarily enabling the stock-launcher customization provider for migration. The superseded personal wallpaper was not reimported because the user changed scope to recommendations.
- Updated in place to 1.3.0.2 with the same signing key; existing token, shelf and settings retained.
- Added freshness tests, including the scarce-fresh-title case and rotation of older fallbacks only when needed.
- Added sports policy tests for India app/channel identity, strict UTC parsing, terminal/postponed events, seven-day horizon, schedule expiry and stale live-status expiry. All passed; all 366 translation keys validated.
- On-TV tests loaded the fresh recommendation row and the real public sports feed. It displayed Brighton and Hove Albion vs Brentford on JioHotstar at 7:30 PM IST on 8 November and Albania vs Finland on SonyLIV at 1:15 AM IST on 13 November. These are feed-provided listings, not a promise of a complete schedule.
- Sports-card launch tests reached `in.startv.hotstar/com.hotstar.MainActivity` and `com.sonyliv/.ui.home.HomeActivity`; Home returned to the launcher. No actual live event was available for a manual Live-badge test; status rules were tested with unit fixtures.
- The main 1.3.0.2 feature build installed and passed those on-TV tests. A final small build adjusts source-credit spacing during focus growth and ensures even a single fresh title stays ahead of older fallbacks; its build and tests pass, but the final install response was lost when the TV's ADB port became unreachable. Exact final APK installation/hash verification is pending reconnection. Final artifact SHA-256: `ffa7847356af9024b9d3251f09e185ed94b6cc480d2d2ee66919744ca4e096c1`.

## Migration and future updates

This APK has its own local signing key, so Android cannot install it over the official APK. Restore Original mode on the TV before uninstalling the official launcher, then install this build and enable it on the TV. Free My TV's settings, imported photos, and saved token will reset; other TV apps and their data are not removed. Re-add the token through the launcher's one-time local key page.

Keep `freemytv.keystore` and the source directory: subsequent custom builds signed with that key can update this build in place. The built-in official APK auto-update path is not available to this custom-signed build; later upstream changes need to be rebuilt with the local key.

## Rebuild

`config.properties` and the signing key are git-ignored. The current config contains no credential. Run `JAVA_HOME=<JDK17 home> ANDROID_HOME=<SDK> bash tools/check.sh android`; the SDK platform used here is Android 34. Do not publish the local signing key or any future private configuration values.
