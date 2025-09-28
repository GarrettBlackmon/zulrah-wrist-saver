package dev.blackmon;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ZulrahWristSaverTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ZulrahWristSaver.class);
		RuneLite.main(args);
	}
}