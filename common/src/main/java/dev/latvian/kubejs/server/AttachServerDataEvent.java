package dev.latvian.kubejs.server;

import dev.latvian.kubejs.script.AttachDataEvent;
import dev.latvian.kubejs.script.DataType;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class AttachServerDataEvent extends AttachDataEvent<ServerJS> {
	public static final Event<Consumer<AttachServerDataEvent>> EVENT = EventFactory.createConsumerLoop(AttachServerDataEvent.class);

	public AttachServerDataEvent(ServerJS s) {
		super(DataType.SERVER, s);
	}
}