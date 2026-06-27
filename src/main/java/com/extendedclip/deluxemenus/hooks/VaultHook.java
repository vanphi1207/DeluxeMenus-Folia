package com.extendedclip.deluxemenus.hooks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

public class VaultHook {
  private static final String PLUGIN_NAME = "DeluxeMenus";
  private static final String VAULT_UNLOCKED_ECONOMY_CLASS = "net.milkbowl.vault2.economy.Economy";

  private final Economy economy;
  private final Object unlockedEconomy;
  private final Method unlockedHas;
  private final Method unlockedWithdraw;
  private final Method unlockedDeposit;
  private final Permission permission;

  public VaultHook() {
    final RegisteredServiceProvider<Economy> rspEconomy = Bukkit.getServer().getServicesManager()
        .getRegistration(Economy.class);
    final RegisteredServiceProvider<Permission> rspPermissions = Bukkit.getServer().getServicesManager()
        .getRegistration(Permission.class);
    final Object[] unlockedRegistration = getVaultUnlockedEconomy();

    economy = rspEconomy == null ? null : rspEconomy.getProvider();
    unlockedEconomy = unlockedRegistration[0];
    unlockedHas = (Method) unlockedRegistration[1];
    unlockedWithdraw = (Method) unlockedRegistration[2];
    unlockedDeposit = (Method) unlockedRegistration[3];
    permission = rspPermissions == null ? null : rspPermissions.getProvider();
  }

  /**
   * Checks if any supported Vault or VaultUnlocked service hook is enabled.
   *
   * @return true if an economy or permission hook is enabled, false otherwise.
   */
  public boolean hooked() {
    return hasEconomy() || hasPermission();
  }

  public boolean hasEconomy() {
    return economy != null || unlockedEconomy != null;
  }

  public boolean hasPermission() {
    return permission != null;
  }

  /**
   * Checks if the player has the amount in their account.
   *
   * @param player the player to check.
   * @param amount the amount to check for.
   * @return true if the economy hook is enabled and player has the amount, false otherwise.
   */
  public boolean hasEnough(@NotNull final Player player, final double amount) {
    if (unlockedEconomy != null) {
      return invokeUnlockedEconomyBoolean(unlockedHas, player.getUniqueId(), amount);
    }
    return economy != null && economy.has(player, amount);
  }

  /**
   * Takes the amount from the player's account.
   * <br>
   * This will do nothing if the economy hook is disabled. You should check {@link #hasEconomy()} before calling this.
   *
   * @param player the player to take from.
   * @param amount the amount to take.
   */
  public void takeMoney(@NotNull final Player player, final double amount) {
    if (unlockedEconomy != null) {
      invokeUnlockedEconomy(unlockedWithdraw, player.getUniqueId(), amount);
      return;
    }
    if (economy == null) return;
    economy.withdrawPlayer(player, amount);
  }

  /**
   * Gives the player the amount.
   * <br>
   * This will do nothing if the economy hook is disabled. You should check {@link #hasEconomy()} before calling this.
   *
   * @param player the player to give to.
   * @param amount the amount to give.
   */
  public void giveMoney(@NotNull final Player player, final double amount) {
    if (unlockedEconomy != null) {
      invokeUnlockedEconomy(unlockedDeposit, player.getUniqueId(), amount);
      return;
    }
    if (economy == null) return;
    economy.depositPlayer(player, amount);
  }

  /**
   * Checks if the player has the permission.
   *
   * @param player the player to check.
   * @param permissionNode the permission to check for.
   * @return true if the permission hook is enabled and player has the permission, false otherwise.
   */
  public boolean hasPermission(@NotNull final Player player, @NotNull final String permissionNode) {
    return this.permission != null && this.permission.has(player, permissionNode);
  }

  /**
   * Take the permission from the player.
   * <br>
   * This will do nothing if the permission hook is disabled. You should check {@link #hasPermission()} before calling this.
   *
   * @param player the player to take from.
   * @param permissionNode the permission to take.
   */
  public void takePermission(@NotNull final Player player, @NotNull final String permissionNode) {
    if (permission == null) return;
    permission.playerRemove(null, player, permissionNode);
  }

  /**
   * Give the player the permission.
   * <br>
   * This will do nothing if the permission hook is disabled. You should check {@link #hasPermission()} before calling this.
   *
   * @param player the player to give to.
   * @param permissionNode the permission to give.
   */
  public void givePermission(@NotNull final Player player, @NotNull final String permissionNode) {
    if (permission == null) return;
    permission.playerAdd(null, player, permissionNode);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object[] getVaultUnlockedEconomy() {
    try {
      final Class<?> economyClass = Class.forName(VAULT_UNLOCKED_ECONOMY_CLASS);
      final RegisteredServiceProvider<?> rspEconomy = Bukkit.getServer().getServicesManager()
          .getRegistration((Class) economyClass);
      if (rspEconomy == null) {
        return new Object[]{null, null, null, null};
      }

      return new Object[]{
          rspEconomy.getProvider(),
          economyClass.getMethod("has", String.class, UUID.class, BigDecimal.class),
          economyClass.getMethod("withdraw", String.class, UUID.class, BigDecimal.class),
          economyClass.getMethod("deposit", String.class, UUID.class, BigDecimal.class)
      };
    } catch (final ReflectiveOperationException ignored) {
      return new Object[]{null, null, null, null};
    }
  }

  private boolean invokeUnlockedEconomyBoolean(@NotNull final Method method, @NotNull final UUID playerId, final double amount) {
    try {
      return Boolean.TRUE.equals(method.invoke(unlockedEconomy, PLUGIN_NAME, playerId, BigDecimal.valueOf(amount)));
    } catch (final ReflectiveOperationException exception) {
      return false;
    }
  }

  private void invokeUnlockedEconomy(@NotNull final Method method, @NotNull final UUID playerId, final double amount) {
    try {
      method.invoke(unlockedEconomy, PLUGIN_NAME, playerId, BigDecimal.valueOf(amount));
    } catch (final ReflectiveOperationException ignored) {
    }
  }
}
