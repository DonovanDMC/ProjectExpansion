package cool.furry.mc.projectexpansion.wiki;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// a command run as the player, with what it says in chat and the chat box with the command still typed in it, cropped to the chat
// the method takes the ServerPlayer, sets up anything the command needs, and returns the command without its /
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    // run as the server's console instead, for what a player's told when someone else uses a command on them
    boolean console() default false;

    // only typed, not run, with the list of what can come next open, like to show a command's subcommands
    boolean suggestions() default false;

    // the chat box with the command in it, or just the messages
    boolean typed() default true;
}
