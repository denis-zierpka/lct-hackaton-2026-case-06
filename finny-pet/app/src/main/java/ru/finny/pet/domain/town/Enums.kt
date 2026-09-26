package ru.finny.pet.domain.town

import kotlinx.serialization.Serializable

/** Перечисления словаря событий «Городка» (GAME_CONCEPT §7.3; TOWN-S0a §1). */
@Serializable
enum class Source { NEED, WANT, RESERVE, SAVINGS, TRANSFER_NEED, TRANSFER_WANT }
@Serializable
enum class PriceRank { CHEAPEST, DEARER }
@Serializable
enum class CoinSource { CHANGE_RETURN, WINDFALL }
@Serializable
enum class Stat { HUNGER, CLEAN, MOOD }
@Serializable
enum class Until { DAY_END, WEEK_END }
@Serializable
enum class TweakDir { NEED, WANT, SAVINGS, RESERVE }
@Serializable
enum class Verdict { GOOD, OK, MISTAKE }
@Serializable
enum class EventKind { ASK, PRICE, OFFER, BREAK, CHANGE, WINDFALL, MISHAP, JOB }
@Serializable
enum class EventStatus { ACTIVE, DONE }
