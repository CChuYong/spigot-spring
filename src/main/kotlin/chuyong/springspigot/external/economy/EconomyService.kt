package chuyong.springspigot.external.economy

import org.bukkit.OfflinePlayer
import java.math.BigDecimal

/**
 * Defines a implementation-agnostic economy service (normally vault)
 */
interface EconomyService {
    /**
     * Deposit an amount to a player
     *
     * @param player to deposit to
     * @param amount Amount to deposit
     */
    fun deposit(player: OfflinePlayer?, amount: BigDecimal)

    /**
     * Withdraw an amount from a player
     *
     * @param player to withdraw from
     * @param amount Amount to withdraw
     */
    fun withdraw(player: OfflinePlayer?, amount: BigDecimal)

    /**
     * Transfer balance from one player to another
     *
     * @param origin      to withdraw from
     * @param destination to deposit to
     * @param amount      Amount to transfer
     */
    fun transfer(origin: OfflinePlayer?, destination: OfflinePlayer?, amount: BigDecimal)

    /**
     * Checks if the player account has the amount
     *
     * @param player to check
     * @param amount to check for
     * @return True if {@param player} has {@param amount}, False else wise
     */
    fun has(player: OfflinePlayer?, amount: BigDecimal): Boolean

    /**
     * Attempts to create a player account for the given player
     *
     * @param player the player to create account
     */
    fun createAccount(player: OfflinePlayer?)

    /**
     * Format amount into a human readable String This provides translation into economy specific formatting to improve consistency between plugins.
     *
     * @param amount to format
     * @return Human readable string describing amount
     */
    fun format(amount: BigDecimal): String?

    /**
     * Gets balance of a player
     *
     * @param player to get the balance
     * @return Amount currently held in players account
     */
    fun getBalance(player: OfflinePlayer?): BigDecimal

    /**
     * Checks if this player has an account on the server yet.
     * This will always return true if the player has joined the server at least once as all major economy plugins
     * auto-generate a player account when the player joins the server
     *
     * @param player to check
     * @return if the player has an account
     */
    fun hasAccount(player: OfflinePlayer?): Boolean
}
