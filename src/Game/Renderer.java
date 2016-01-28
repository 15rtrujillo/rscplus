/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package Game;

import Client.Settings;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

public class Renderer
{
	public static void init()
	{
		width = Game.instance.getContentPane().getWidth();
		height = Game.instance.getContentPane().getHeight();
		height_client = height - 12;
		game_image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// Load fonts
		try
		{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			InputStream is = Settings.getResourceAsStream("/assets/Helvetica-Bold.ttf");
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			ge.registerFont(font);
			font_main = font.deriveFont(Font.PLAIN, 11.0f);
			font_big = font.deriveFont(Font.PLAIN, 22.0f);

			is = Settings.getResourceAsStream("/assets/TimesRoman.ttf");
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, is));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// Load images
		try
		{
			image_border = ImageIO.read(Settings.getResource("/assets/border.png"));
			image_bar_frame = ImageIO.read(Settings.getResource("/assets/bar.png"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void resize(int w, int h)
	{
		// TODO: Full resizable
		//
		// 1. Need to resize the client's game Image (via it's ImageConsumer)
		// 2. Need to resize the renderer's pixel buffer
		// 3. Need to pass that new buffer to the camera
		// 4. Run a certain camera method (can't remember off the top of my head, it's in one of the hooks), it'll set it up for the new size
		// 5. Move all menus relative to the new size (the top right menus, the chat, all of that good stuff)
		//
		// That may not be all, but it's a start.
		// There may be some shortcuts calling some of the init methods for the HUD components in client.class
		//
		// As of right now, all of this isn't easy to add because of the lack of anything menu related for our reflection,
		// but as soon as it's all set, it will not be hard to do at all.
		//
		// Resizable in it's most basic form is a joke, don't let anybody ever fool you and say they worked hard implementing it.
		//	A lot of the RSC engine's drawing is based on the client's current width/height, so when you resize it, it works right away.
		//	Now, there are a few exceptions to this. The right click menu has static coordinates for it's bounds, and can only
		//	be fixed patching the bytecode. The menus (top right bar, chat, etc.) also use static coordinates, so they all must
		//	be patched as well. In rscplus, we patch it all using the current windows width/height, so most things will work right away.
		//	For example, when full resizable is added, any corrected UI elements like loading/logging out will be 100% working.
		//
		// 1. Set client class width/height
		// 2. Patch hardcoded mouse coordinates for right click menu
		// 3. Patch renderer "~###~" text coords to support 4 digit dimensions "~####~" (old method is meant for the 512x346 game window size)
		//	NOTE: Technically we can't go past 9999xX because of this, but it includes up to 8K resolution.
		// 4. Patch the friend's list remove text coordinate with the new one
		// 5. Patch friend's list mouse coordinates for the new size
		//
		// I am absolutely amazed that these so-called "client hackers" that are just getting this feature couldn't do this until after rscplus
		// was released. I don't know every client mod out there, but I do know of at least one that got resizable and asks for money.
		//
		// I'm surprised there was a demand for it. Seeing as none of their users play long enough to need to see the screen.
		//
		// ~ OrNoX
	}

	public static void present(Graphics g, Image image)
	{
		// Update timing
		long new_time = System.currentTimeMillis();
		delta_time = (float)(new_time - time) / 1000.0f;
		time = new_time;
		alpha_time =  0.25f + (((float)Math.sin(time / 100) + 1.0f) / 2.0f * 0.75f);

		// Run other parts update methods
		Client.update();

		Graphics2D g2 = (Graphics2D)game_image.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(font_main);

		g2.drawImage(image, 0, 0, null);
		g2.drawImage(image_border, 512, height - 13, width - 512, 13, null);

		if(Client.state == Client.STATE_GAME)
		{
			// TODO: Inventory max is hardcoded here, I think there's a variable somewhere
			// in client.class that contains the max inventory slots
			drawShadowText(g2, Client.inventory_count + "/" + 30, width - 19, 17, color_text, true);

			int percentHP = 0;
			int percentPrayer = 0;
			float alphaHP = 1.0f;
			float alphaPrayer = 1.0f;
			float alphaFatigue = 1.0f;
			Color colorHP = color_hp;
			Color colorPrayer = color_prayer;
			Color colorFatigue = color_fatigue;

			if(Client.getBaseLevel(Client.SKILL_HP) > 0)
			{
				percentHP = Client.getLevel(Client.SKILL_HP) * 100 / Client.getBaseLevel(Client.SKILL_HP);
				percentPrayer = Client.getLevel(Client.SKILL_PRAYER) * 100 / Client.getBaseLevel(Client.SKILL_PRAYER);
			}

			if(percentHP < 30)
			{
				colorHP = color_low;
				alphaHP = alpha_time;
			}

			if(percentPrayer < 30)
			{
				colorPrayer = color_low;
				alphaPrayer = alpha_time;
			}

			if(Client.getFatigue() >= 80)
			{
				colorFatigue = color_low;
				alphaFatigue = alpha_time;
			}

			// Draw HP, Prayer, Fatigue overlay
			int x = 24;
			int y = 138;
			if(width < 800)
			{
				setAlpha(g2, alphaHP);
				drawShadowText(g2, "Hits: " + Client.current_level[Client.SKILL_HP] + "/" + Client.base_level[Client.SKILL_HP], x, y, colorHP, false); y += 16;
				setAlpha(g2, alphaPrayer);
				drawShadowText(g2, "Prayer: " + Client.current_level[Client.SKILL_PRAYER] + "/" + Client.base_level[Client.SKILL_PRAYER], x, y, colorPrayer, false); y += 16;
				setAlpha(g2, alphaFatigue);
				drawShadowText(g2, "Fatigue: " + Client.getFatigue() + "/100", x, y, colorFatigue, false); y += 16;
			}
			else
			{
				int barSize = 4 + image_bar_frame.getWidth(null);
				int x2 = width - (4 + barSize);
				int y2 = height - image_bar_frame.getHeight(null);

				drawBar(g2, image_bar_frame, x2, y2, colorFatigue, alphaFatigue, Client.getFatigue(), 100);
				x2 -= barSize;

				drawBar(g2, image_bar_frame, x2, y2, colorPrayer, alphaPrayer, Client.current_level[Client.SKILL_PRAYER], Client.base_level[Client.SKILL_PRAYER]);
				x2 -= barSize;

				drawBar(g2, image_bar_frame, x2, y2, colorHP, alphaHP, Client.current_level[Client.SKILL_HP], Client.base_level[Client.SKILL_HP]);
				x2 -= barSize;
			}

			// Draw under combat style info
			for(int i = 0; i < 18; i++)
			{
				if(Client.current_level[i] != Client.base_level[i] && (i != Client.SKILL_HP && i != Client.SKILL_PRAYER))
				{
					int diff = Client.current_level[i] - Client.base_level[i];
					Color color = color_low;

					String boost = "" + diff;
					if(diff > 0)
					{
						boost = "+" + boost;
						color = color_hp;
					}

					drawShadowText(g2, boost, x, y, color, false);
					drawShadowText(g2, Client.skill_name[i], x + 32, y, color, false);
					y += 14;
				}
			}

			Client.xpdrop_handler.draw(g2);
			Client.xpbar.draw(g2);

			if(Settings.DEBUG)
			{
				y = 32;

				// Draw Skills
				for(int i = 0; i < 18; i++)
				{
					drawShadowText(g2, Client.skill_name[i] + " (" + i + "): " + Client.current_level[i] + "/" + Client.base_level[i] + " (" + Client.getXP(i) + " xp)", 32, y, color_text, false);
					y += 16;
				}

				// Draw Fatigue
				y += 16;
				drawShadowText(g2, "Fatigue: " + ((float)Client.fatigue * 100.0f / 750.0f), 32, y, color_text, false); y += 16;

				// Draw Mouse Info
				y += 16;
				drawShadowText(g2, "Mouse Position: " + MouseHandler.x + ", " + MouseHandler.y, 32, y, color_text, false); y += 16;

				// Draw camera info
				y += 16;
				drawShadowText(g2, "Camera Rotation: " + Camera.rotation, 32, y, color_text, false); y += 16;
				drawShadowText(g2, "Camera Zoom: " + Camera.zoom, 32, y, color_text, false); y += 16;
				drawShadowText(g2, "Camera Distance1: " + Camera.distance1, 32, y, color_text, false); y += 16;
				drawShadowText(g2, "Camera Distance2: " + Camera.distance2, 32, y, color_text, false); y += 16;
				drawShadowText(g2, "Camera Distance3: " + Camera.distance3, 32, y, color_text, false); y += 16;
				drawShadowText(g2, "Camera Distance4: " + Camera.distance4, 32, y, color_text, false); y += 16;
			}

			g2.setFont(font_big);
			if(Settings.FATIGUE_ALERT && Client.getFatigue() >= 100)
			{
				setAlpha(g2, alpha_time);
				drawShadowText(g2, "FATIGUED", width / 2, height / 2, color_low, true);
				setAlpha(g2, 1.0f);
			}
		}

		g2.dispose();

		// Right now is a good time to take a screenshot if one is requested
		if(screenshot)
		{
			try
			{
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
				String fname = Settings.Dir.SCREENSHOT + "/" + "Screenshot from " + format.format(new Date()) + ".png";
				ImageIO.write(game_image, "png", new File(fname));
				Client.displayMessage("@cya@Screenshot saved to '" + fname + "'", Client.CHAT_NONE);
			}
			catch(Exception e) {}
			screenshot = false;
		}

		g.drawImage(game_image, 0, 0, null);

		frames++;
		if(time > fps_timer)
		{
			fps = frames;
			frames = 0;
			fps_timer = time + 1000;

			Game.instance.setTitle("FPS: " + fps);
		}
	}

	public static void drawBar(Graphics2D g, Image image, int x, int y, Color color, float alpha, int value, int total)
	{
		// Prevent divide by zero
		if(total == 0)
			return;

		int width = image.getWidth(null) - 2;
		int percent = value * width / total;

		g.setColor(color_shadow);
		g.fillRect(x + 1, y, width, image.getHeight(null));

		g.setColor(color);
		setAlpha(g, alpha);
		g.fillRect(x + 1, y, percent, image.getHeight(null));
		setAlpha(g, 1.0f);

		g.drawImage(image_bar_frame, x, y, null);
		drawShadowText(g, value + "/" + total, x + (image.getWidth(null) / 2), y + (image.getHeight(null) / 2) - 2, color_text, true);
	}

	public static void setAlpha(Graphics2D g, float alpha)
	{
		g.setComposite(AlphaComposite.SrcOver.derive(alpha));
	}

	public static void drawShadowText(Graphics2D g, String text, int x, int y, Color textColor, boolean center)
	{
		int textX = x;
		int textY = y;
		if(center)
		{
			Dimension bounds = getStringBounds(g, text);
			textX -= (bounds.width / 2);
			textY += (bounds.height / 2);
		}

		g.setColor(color_shadow);
		g.drawString(text, textX + 1, textY);
		g.drawString(text, textX - 1, textY);
		g.drawString(text, textX, textY + 1);
		g.drawString(text, textX, textY - 1);

		g.setColor(textColor);
		g.drawString(text, textX, textY);
	}

	public static void takeScreenshot()
	{
		screenshot = true;
	}

	private static Dimension getStringBounds(Graphics2D g, String str)
	{
		FontRenderContext context = g.getFontRenderContext();
		Rectangle2D bounds = g.getFont().getStringBounds(str, context);
		return new Dimension((int)bounds.getWidth(), (int)bounds.getHeight());
	}

	public static int width;
	public static int height;
	public static int height_client;
	public static int fps;
	public static float alpha_time;
	public static float delta_time;
	public static long time;

	private static Font font_main;
	private static Font font_big;

	private static int frames = 0;
	private static long fps_timer = 0;
	private static boolean screenshot = false;

	public static Color color_text = new Color(240, 240, 240);
	public static Color color_shadow = new Color(15, 15, 15);
	public static Color color_gray = new Color(60, 60, 60);
	public static Color color_hp = new Color(0, 210, 0);
	public static Color color_fatigue = new Color(210, 210, 0);
	public static Color color_prayer = new Color(160, 160, 210);
	public static Color color_low = new Color(255, 0, 0);

	public static Image image_border;
	public static Image image_bar_frame;
	private static BufferedImage game_image;
}
