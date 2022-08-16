package dev.intelligentcreations.pingme;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingMe implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Ping Me");

	@Override
	public void onInitialize() {
		LOGGER.info("Initialized.");
	}
}
