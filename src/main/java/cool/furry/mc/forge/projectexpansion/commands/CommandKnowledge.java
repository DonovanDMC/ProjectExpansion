package cool.furry.mc.forge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.util.ColorStyle;
import cool.furry.mc.forge.projectexpansion.util.Lang;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CommandKnowledge {
    private enum ActionType {
        LEARN,
        UNLEARN,
        CLEAR,
        TEST
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("knowledge")
            .requires((source) -> source.hasPermission(2))
            .then(Commands.literal("clear")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes((ctx) -> CommandKnowledge.handle(ctx, ActionType.CLEAR))
                )
            )
            .then(Commands.literal("learn")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("item", ItemArgument.item(buildContext))
                        .executes((ctx) -> CommandKnowledge.handle(ctx, ActionType.LEARN))
                    )
              )
            )
            .then(Commands.literal("unlearn")
                 .then(Commands.argument("player", EntityArgument.player())
                     .then(Commands.argument("item", ItemArgument.item(buildContext))
                         .executes((ctx) -> CommandKnowledge.handle(ctx, ActionType.UNLEARN))
                    )
                )
            )
            .then(Commands.literal("test")
                 .then(Commands.argument("player", EntityArgument.player())
                     .then(Commands.argument("item", ItemArgument.item(buildContext))
                         .executes((ctx) -> CommandKnowledge.handle(ctx, ActionType.TEST))
                    )
                )
            );

        dispatcher.register(cmd);
    }

    private static Component getSourceName(CommandSourceStack source) {
        try {
            return source.getPlayerOrException().getDisplayName();
        } catch (CommandSyntaxException e) {
            return Lang.Commands.CONSOLE.translateColored(ChatFormatting.RED);
        }
    }

    private static boolean compareUUID(CommandSourceStack source, ServerPlayer player) {
        try {
            return source.getPlayerOrException().getUUID().equals(player.getUUID());
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int handle(CommandContext<CommandSourceStack> ctx, ActionType action) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        boolean isSelf = compareUUID(ctx.getSource(), player);
        if(action == ActionType.CLEAR) {
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            if(provider == null) {
                ctx.getSource().sendFailure(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(ChatFormatting.RED, player.getDisplayName()));
                return 0;
            }
            if(provider.getKnowledge().size() == 0) {
                if(isSelf) {
                    ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_CLEAR_FAIL_SELF.translateColored(ChatFormatting.RED));
                } else {
                    ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_CLEAR_FAIL.translateColored(ChatFormatting.RED, player.getDisplayName()));
                }
                return 0;
            }
            provider.clearKnowledge();
            provider.sync(player);
            if (compareUUID(ctx.getSource(), player)) {
                ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_CLEAR_SUCCESS_SELF.translateColored(ChatFormatting.GREEN), false);
            } else {
                ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_CLEAR_SUCCESS.translateColored(ChatFormatting.GREEN, player.getDisplayName()), true);
                if(Config.notifyCommandChanges.get()) {
                    player.sendSystemMessage(Lang.Commands.KNOWLEDGE_CLEAR_NOTIFICATION.translate(getSourceName(ctx.getSource())));;
                }
            }
            return 1;
        }
        Item item = ItemArgument.getItem(ctx, "item").getItem();

        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if(provider == null) {
            ctx.getSource().sendFailure(Lang.FAILED_TO_GET_KNOWLEDGE_PROVIDER.translateColored(ChatFormatting.RED, player.getDisplayName()));
            return 0;
        }
        IEMCProxy proxy = ProjectEAPI.getEMCProxy();
        if (!proxy.hasValue(item)) {
            ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_INVALID.translate());
            return 0;
        }
        int response = 1;
        switch (action) {
            case LEARN -> {
                if (!provider.addKnowledge(ItemInfo.fromItem(item))) {
                    response = 0;
                    if (isSelf) {
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_LEARN_FAIL_SELF.translateColored(ChatFormatting.RED, new ItemStack(item).getDisplayName()));
                    } else {
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_LEARN_FAIL.translateColored(ChatFormatting.RED, player.getDisplayName(), new ItemStack(item).getDisplayName()));
                    }
                } else {
                    if (isSelf) {
                        ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_LEARN_SUCCESS_SELF.translateColored(ChatFormatting.GREEN, new ItemStack(item).getDisplayName()), false);
                    } else {
                        ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_LEARN_SUCCESS.translateColored(ChatFormatting.GRAY, player.getDisplayName(), new ItemStack(item).getDisplayName()), true);
                        if (Config.notifyCommandChanges.get()) {
                            player.sendSystemMessage(Lang.Commands.KNOWLEDGE_LEARN_NOTIFICATION.translateColored(ChatFormatting.GRAY, new ItemStack(item).getDisplayName(), getSourceName(ctx.getSource())));
                        }
                    }
                }
            }
            case UNLEARN -> {
                if (!provider.removeKnowledge(ItemInfo.fromItem(item))) {
                    response = 0;
                    if (isSelf) {
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_UNLEARN_FAIL_SELF.translateColored(ChatFormatting.RED, new ItemStack(item).getDisplayName()));
                    } else {
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_UNLEARN_FAIL.translateColored(ChatFormatting.RED, player.getDisplayName(), new ItemStack(item).getDisplayName()));
                    }
                } else {
                    if (isSelf) {
                        ctx.getSource().sendSuccess(Component.translatable("command.projectexpansion.knowledge.unlearn.successSelf", new ItemStack(item).getDisplayName()).setStyle(ColorStyle.GREEN), false);
                        ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_UNLEARN_SUCCESS_SELF.translateColored(ChatFormatting.GREEN, new ItemStack(item).getDisplayName()), false);
                    } else {
                        ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_UNLEARN_SUCCESS.translateColored(ChatFormatting.GREEN, player.getDisplayName(), new ItemStack(item).getDisplayName()), true);
                        if (Config.notifyCommandChanges.get()) {
                            player.sendSystemMessage(Lang.Commands.KNOWLEDGE_UNLEARN_NOTIFICATION.translateColored(ChatFormatting.GRAY, new ItemStack(item).getDisplayName(), getSourceName(ctx.getSource())));
                        }
                    }
                }
            }
            case TEST -> {
                if (!provider.hasKnowledge(ItemInfo.fromItem(item))) {
                    response = 0;
                    if (isSelf) {
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_TEST_FAIL_SELF.translateColored(ChatFormatting.RED, new ItemStack(item).getDisplayName()));
                    } else {
                        ctx.getSource().sendFailure(Lang.Commands.KNOWLEDGE_TEST_FAIL.translateColored(ChatFormatting.RED, player.getDisplayName(), new ItemStack(item).getDisplayName()));
                    }
                } else {
                    if (isSelf) {
                        ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_TEST_SUCCESS_SELF.translateColored(ChatFormatting.GREEN, new ItemStack(item).getDisplayName()), false);
                    } else {
                        ctx.getSource().sendSuccess(Lang.Commands.KNOWLEDGE_TEST_SUCCESS.translateColored(ChatFormatting.GREEN, player.getDisplayName(), new ItemStack(item).getDisplayName()), false);
                    }
                }
            }
        }
        if(response == 1 && action != ActionType.TEST) provider.syncKnowledgeChange(player, NBTManager.getPersistentInfo(ItemInfo.fromItem(item)), true);

        return response;
    }
}
