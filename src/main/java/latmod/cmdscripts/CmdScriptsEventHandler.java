package latmod.cmdscripts;

import java.io.File;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import ftb.lib.*;
import ftb.lib.api.EventFTBWorldServer;
import latmod.lib.*;
import net.minecraft.command.*;
import net.minecraft.util.*;

public class CmdScriptsEventHandler
{
	public static final FastMap<String, ScriptFile> files = new FastMap<String, ScriptFile>();
	public static final FastList<ScriptInstance> running = new FastList<ScriptInstance>();
	private static final FastList<ScriptInstance> pending = new FastList<ScriptInstance>();
	private static int nextScriptID = 0;
	
	@SubscribeEvent
	public void onWorldLoaded(EventFTBWorldServer e)
	{ reload(FTBLib.getServer()); }
	
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent e) // FTBLibEventHandler
	{
		if(!e.world.isRemote && e.side == Side.SERVER && e.phase == TickEvent.Phase.END && e.type == TickEvent.Type.WORLD && e.world.provider.dimensionId == 0)
		{
			if(!pending.isEmpty())
			{
				running.addAll(pending);
				pending.clear();
			}
			
			if(!running.isEmpty())
			{
				for(int i = running.size() - 1; i >= 0; i--)
				{
					ScriptInstance inst = running.get(i);
					while(!inst.stopped())
					{
						try { inst.runCurrentLine(); }
						catch(Exception ex)
						{
							inst.stop();
							BroadcastSender.inst.addChatMessage(new ChatComponentText("Script '" + inst.file.ID + "' at " + LMStringUtils.stripI(inst.sender.pos.posX, inst.sender.pos.posY, inst.sender.pos.posZ) + " crashed at line " + (inst.currentLine() + 1) + ":"));
							
							if(ex instanceof CommandException)
							{
								CommandException cx = (CommandException)ex;
								BroadcastSender.inst.addChatMessage(new ChatComponentTranslation(cx.getMessage(), cx.getErrorOjbects()));
							}
							else
								BroadcastSender.inst.addChatMessage(new ChatComponentText(ex.toString()));
							ex.printStackTrace();
						}
						
						if(inst.isSleeping()) break;
					}
					if(inst.stopped()) running.remove(i);
				}
			}
		}
	}
	
	public static ScriptInstance runScript(ScriptFile file, ICommandSender sender, String[] args)
	{
		ScriptInstance inst = new ScriptInstance(++nextScriptID, file, sender, args);
		pending.add(inst);
		return inst;
	}
	
	public static void reload(ICommandSender sender)
	{
		pending.clear();
		running.clear();
		
		ScriptInstance.clearGlobalVariables(FTBLib.getServer());
		
		files.clear();
		
		File folder = new File(sender.getEntityWorld().getSaveHandler().getWorldDirectory(), "/latmod/cmd_scripts/");
		if(!folder.exists()) folder.mkdirs();
		else
		{
			File[] f = folder.listFiles();
			
			for(File f1 : f)
			{
				if(f1.isFile() && f1.canRead() && f1.getName().endsWith(".script"))
				{
					try
					{
						FastList<String> l = LMFileUtils.load(f1);
						ScriptFile file = new ScriptFile(PreUpdate.getRawFileName(f1));
						file.compile(l);
						files.put(file.ID, file);
					}
					catch(Exception ex)
					{ ex.printStackTrace(); }
				}
			}
		}
		
		ScriptFile.startupFile = files.get("startup");
		ScriptFile.globalVariablesFile = files.get("global_variables");
		
		if(ScriptFile.startupFile != null) runScript(ScriptFile.startupFile, FTBLib.getServer(), new String[0]);
		sender.addChatMessage(new ChatComponentText("CommandScripts reloaded!"));
	}
}