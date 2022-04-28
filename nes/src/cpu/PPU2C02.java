package cpu;

import java.util.Arrays;
import java.util.Random;
import PixelEngine.Pixel;
import cpu.Cartridge.NametableMirror;
//import nesPixelEngine.Sprite;
import usingSlickTest.Sprite;

/**
 * <p>
 * The PPU addresses a 16kB space, $0000-$3FFF, completely separate from the
 * CPU's address bus. It is either directly accessed by the PPU itself, or via
 * the CPU with memory mapped registers at $2006 and $2007.
 * </p>
 * 
 * <br>
 * <p>
 * The NES has 2kpB of RAM dedicated to the PPU, normally mapped to the
 * nametable address space from $2000-$3FFF, buy this can be rerouted through
 * custom cartridge wiring.
 * </p>
 * 
 * @author jorgejimenez
 *
 */
public class PPU2C02 {

	// •–––––––––––––––––––––––––––•
	// | OAM
	// •–––––––––––––––––––––––––––•
	public int OAM[] = new int[64 * 4];
	public int[] spriteScanline = new int[8 * 4];
	public int spriteCount;

	public int[] spriteShifterPatternLo = new int[8];
	public int[] spriteShifterPatternHi = new int[8];
	private boolean spriteZeroHitPossible = false;
	private boolean spriteZeroBeingRendered = false;

	// o-------------------------o
	// | DEBUGGING
	// | STUFF
	// o-------------------------o
	private Random rand = new Random();
	public int[] screenTileIds = new int[32 * 30];
	public int[] activeNameTable = new int[0x3ff + 1];

	// o-------------------------o
	// | RENDERING
	// | STUFF
	// o-------------------------o
	private final Pixel[] colorPallet = new Pixel[0x3F + 1];
	private Sprite screen;
	private Sprite[] patternTblScreen = {new Sprite(128, 128), new Sprite(128, 128)};
	private Sprite nameTableScreen;

	public static int PIXEL_SIZE = 1;
	private int pixelSize = PPU2C02.PIXEL_SIZE;

	// o-------------------------o
	// | PPU RENDERING
	// o-------------------------o
	private int bgNextTileId = 0;
	private int bgNextTileAttbr = 0;
	private int bgNextTileLsb = 0;
	private int bgNextTileMsb = 0;
	private int bgShifterPatternLO = 0;
	private int bgShifterPatternHI = 0;
	private int bgShifterAttributeLO = 0;
	private int bgShifterAttributeHI = 0;

	// o-------------------------o
	// | PPU I/O PORTS
	// o-------------------------o
	private final int PPUCTRL = 0x0000;
	private final int PPUMASK = 0x0001;
	private final int PPUSTATUS = 0x0002;
	private final int OAMADDR = 0x0003;
	private final int OAMDATA = 0x0004;
	private final int PPUSCROLL = 0x0005;
	private final int PPUADDR = 0x0006;
	private final int PPUDATA = 0x0007;

	// PPU writing and reading.
	private int addressLatch = 0x00;
	private int ppuDataBuffer = 0x00;
	private int fineX = 0x00;

	public boolean nmi = false;
	// o---------------------------o
	// | PPU I/O REGISTER INSTANCES
	// o---------------------------o
	private PPUIO.Status status;
	private PPUIO.Mask mask;
	private PPUIO.Controller control;
	// private PPUIO.Status status;
	private PPUIO.loopyReg loopyV;
	private PPUIO.loopyReg loopyT;

	// Reference to the cartridge.
	private Cartridge cart;

	// •––––––––––––––––––––––––•
	// | PPU RAM
	// •––––––––––––––––––––––––•
	private int[][] patternTable;
	private int[][] nametable;
	private int[] palletRam;

	private int scanline = 0;
	private int cycle = 0;

	private boolean frameComplete;

	public boolean scanlineComplete = false;

	private int frameCount = 0;
	private Bus bus;

	public PPU2C02(Cartridge cart) {

		// this.screen = screen;
		screen = new Sprite(0, 0, 256 * pixelSize, 240 * pixelSize);

		// patternTable = new int[0x2000];
		// nametable = new int[0x400 * 4];
		patternTable = new int[2][4096];// for experimentation (j a v i d x9)
		nametable = new int[2][1024];
		palletRam = new int[32];

		this.cart = cart;

		// Initiate the PPU I/O registers
		status = new PPUIO.Status();
		mask = new PPUIO.Mask();
		control = new PPUIO.Controller();
		loopyT = new PPUIO.loopyReg();
		loopyV = new PPUIO.loopyReg();

		status.set(0);
		createColorPallet();

	}

	/**
	 * <p>
	 * Performs a read originated from a {@code CPU6502}. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 * @return TODO:
	 */
	public int cpuRead(int addr, boolean readOnly) {
		// TODO Auto-generated method stub
		int data = 0;
		switch (addr) {
			case PPUCTRL :
				break;
			case PPUMASK :
				break;
			case PPUSTATUS :
				data = (status.register() & 0xe0) | (ppuDataBuffer & 0x1f);
				status.setVerticalBlank(false);
				addressLatch = 0;
				break;
			case OAMADDR :
				break;
			case OAMDATA :
				break;
			case PPUSCROLL :
				break;
			case PPUADDR :
				break;
			case PPUDATA :
				data = ppuDataBuffer;
				ppuDataBuffer = ppuRead(loopyV.register, false);

				if (loopyV.register > 0x3f00)
					data = ppuDataBuffer;

				loopyV.register += (control.incrementMode() == 1) ? 32 : 1;
				break;
		}
		return data;
	}

	/**
	 * <p>
	 * Performs a write originated from a {@code CPU6502}. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 */
	public void cpuWrite(int addr, int data) {
		// TODO Auto-generated method stub
		switch (addr) {
			case PPUCTRL :
				control.set(data);

				loopyT.nametableX(control.nametableX());
				loopyT.nametableY(control.nametableY());
				// control.setEnableNMI(true);
				break;
			case PPUMASK :
				mask.set(data);
				break;
			case PPUSTATUS :
				break;
			case OAMADDR :
				break;
			case OAMDATA :
				break;
			case PPUSCROLL :
				if (addressLatch == 0) {
					fineX = data & 0x07;
					loopyT.coarseX(data >> 3);
					addressLatch = 1;
				} else {
					loopyT.fineY(data & 0x07);
					loopyT.coarseY(data >> 3);
					addressLatch = 0;

				}

				break;
			case PPUADDR :
				if (addressLatch == 0) {
					loopyT.register(((data & 0x3f) << 8) | (loopyT.register & 0x00ff));
					addressLatch = 1;
				} else {
					loopyT.register((loopyT.register & 0xff00) | data);
					loopyV.register(loopyT.register);
					addressLatch = 0;

				}
				break;
			case PPUDATA :
				ppuWrite(loopyV.register, data);
				// DBUG:

				loopyV.register += (control.incrementMode() == 1) ? 32 : 1;
				break;
		}

	}

	/**
	 * <p>
	 * Reads using the {@code PPU2C02}'s own bus. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 * @return TODO:
	 */
	public int ppuRead(int addr, boolean readOnly) {
		// TODO Auto-generated method stub

		int data = 0x00;
		addr &= 0x3fff; // wrap address between 0x00 and 0x3fff

		data = cart.ppuRead(addr);
		if (data != -1) {
			return data;
		}

		data = 0x00;
		if (addr >= 0x0000 && addr <= 0x1fff) {
			data = patternTable[(addr & 0x1000) >> 12][addr & 0x0fff];
			return data;
		}

		if (addr >= 0x2000 && addr <= 0x3eff) {

			addr &= 0x0fff;

			if (cart.getMirroring() == NametableMirror.VERTICAL) {
				if (addr >= 0x0000 && addr <= 0x03ff) {
					data = nametable[0][addr & 0x03ff];
					return data;
				}
				if (addr >= 0x0400 && addr <= 0x07ff) {
					data = nametable[1][addr & 0x03ff];
					return data;
				}
				if (addr >= 0x0800 && addr <= 0x0bff) {
					data = nametable[0][addr & 0x03ff];
					return data;
				}
				if (addr >= 0x0c00 && addr <= 0x0fff) {
					data = nametable[1][addr & 0x03ff];
					return data;
				}

			} else if (cart.getMirroring() == NametableMirror.HORIZONTAL) {
				if (addr >= 0x0000 && addr <= 0x03ff) {
					data = nametable[0][addr & 0x03ff];
					return data;
				}
				if (addr >= 0x0400 && addr <= 0x07ff) {
					data = nametable[0][addr & 0x03ff];
					return data;
				}
				if (addr >= 0x0800 && addr <= 0x0bff) {
					data = nametable[1][addr & 0x03ff];
					return data;
				}
				if (addr >= 0x0c00 && addr <= 0x0fff) {
					data = nametable[1][addr & 0x03ff];
					return data;
				}

			}
		}

		if (addr >= 0x3f00 && addr <= 0x3fff) {
			addr &= 0x001f;
			if (addr == 0x0010)
				addr = 0x0000;
			if (addr == 0x0014)
				addr = 0x0004;
			if (addr == 0x0018)
				addr = 0x0008;
			if (addr == 0x001c)
				addr = 0x000c;
			data = palletRam[addr];

			return data;
		}

		return data;
	}

	/**
	 * <p>
	 * Writes using the {@code PPU2C02}'s own bus. TODO:
	 * </p>
	 * 
	 * @param addr
	 * @param readOnly
	 */
	public void ppuWrite(int addr, int data) {
		// TODO Auto-generated method stub

		addr &= 0x3fff;
		if (cart.ppuWrite(addr, data) == 0) {
			// TODO: We might want to do something with these later.
			return;
		}

		if (addr >= 0x0000 && addr <= 0x1fff) {
			patternTable[(addr & 0x1000) >> 12][addr & 0x0fff] = data;
			return;
		}

		if (addr >= 0x2000 && addr <= 0x3eff) {

			addr &= 0x0fff;
			int top = (addr & 0x03ff) + 1; // DEBUG:

			if (cart.getMirroring() == NametableMirror.VERTICAL) {
				if (addr >= 0x0000 && addr <= 0x03ff) {
					nametable[0][addr & 0x03ff] = data;
					return;
				}
				if (addr >= 0x0400 && addr <= 0x07ff) {
					nametable[1][addr & 0x03ff] = data;
					return;
				}
				if (addr >= 0x0800 && addr <= 0x0bff) {
					nametable[0][addr & 0x03ff] = data;
					return;
				}
				if (addr >= 0x0c00 && addr <= 0x0fff) {
					nametable[1][addr & 0x03ff] = data;
					return;
				}

			} else if (cart.getMirroring() == NametableMirror.HORIZONTAL) {
				if (addr >= 0x0000 && addr <= 0x03ff) {
					nametable[0][addr & 0x03ff] = data;
				}
				if (addr >= 0x0400 && addr <= 0x07ff) {
					nametable[0][addr & 0x03ff] = data;
				}
				if (addr >= 0x0800 && addr <= 0x0bff) {
					nametable[1][addr & 0x03ff] = data;
				}
				if (addr >= 0x0c00 && addr <= 0x0fff) {
					nametable[1][addr & 0x03ff] = data;
				}
			}
			// Hexdump.print(0, top, nametable[0]);
			return;
		}

		if (addr >= 0x3f00 && addr <= 0x3fff) {
			addr &= 0x001f;
			if (addr == 0x0010)
				addr = 0x0000;
			if (addr == 0x0014)
				addr = 0x0004;
			if (addr == 0x0018)
				addr = 0x0008;
			if (addr == 0x001c)
				addr = 0x000c;
			palletRam[addr] = data;
			return;
		}
	}

	public void clock() {

		if (scanline >= -1 && scanline < 240) {

			if (scanline == 0 && cycle == 0) {
				cycle = 1;
			}

			if (scanline == -1 && cycle == 1) {
				status.setVerticalBlank(false);
				status.setSpriteZeroHit(false);
				status.setSpriteOveflow(false);

				for (int i = 0; i < 8; i++) {
					spriteShifterPatternLo[i] = 0;
					spriteShifterPatternHi[i] = 0;
				}

			}

			if ((cycle >= 2 && cycle < 258) || (cycle >= 321 && cycle < 338)) {

				updateShifters();
				// System.out.println("pixel: " + (cycle - 1) % 8);
				switch ((cycle - 1) % 8) {
					case 0 :
						loadBackgroundShifters();
						bgNextTileId = ppuRead(0x2000 | (loopyV.register & 0x0fff), false);
						break;
					case 2 :
						// int nametableY = loopyV.nametableY();
						// int nametableX = loopyV.nametableX();

						bgNextTileAttbr = ppuRead(0x23c0 | (loopyV.nametableY() << 11) | (loopyV.nametableX() << 10)
								| ((loopyV.coarseY() >> 2) << 3) | (loopyV.coarseX() >> 2), false);

						if ((loopyV.coarseY() & 0x02) == 0x02)
							bgNextTileAttbr >>= 4;
						if ((loopyV.coarseX() & 0x02) == 0x02)
							bgNextTileAttbr >>= 2;
						bgNextTileAttbr &= 0x03;
						break;
					case 4 :
						int i = 0;
						bgNextTileLsb = ppuRead(
								(control.patternBackground() << 12) + (bgNextTileId << 4) + (loopyV.fineY()) + 0,
								false);
						// bgNextTileMsb =
						// this.patternTable[control.patternBackground()][
						// (bgNextTileId << 4) + (loopyV.fineY()) + 0];
						break;

					case 6 :
						i = 0;
						bgNextTileMsb = ppuRead(
								(control.patternBackground() << 12) + (bgNextTileId << 4) + (loopyV.fineY()) + 8,
								false);

						// bgNextTileMsb =
						// this.patternTable[control.patternBackground()][
						// (bgNextTileId << 4) + (loopyV.fineY()) + 8];
						break;

					case 7 :
						incrementScrollX();
						break;
				}
			}

			if (cycle == 256) {
				incrementScrollY();
			}
			if (cycle == 257) {
				loadBackgroundShifters();
				transferAddressX();
			}

			if (cycle == 338 || cycle == 340) {
				bgNextTileId = ppuRead(0x2000 | (loopyV.register & 0x0fff), false);
			}

			if (scanline == -1 && cycle >= 280 && cycle < 305) {
				transferAddressY();

			}

			// Foreground

			if (cycle == 257 && scanline >= 0) {

				Arrays.fill(spriteScanline, 0xff);
				for (int i = 0; i < 8; i++) {
					spriteShifterPatternLo[i] = 0;
					spriteShifterPatternHi[i] = 0;
				}
				spriteCount = 0;
				int spriteSize = (control.spriteSize() == 1) ? 16 : 8;
				int OAMEntry = 0;
				spriteZeroHitPossible = false;
				// Hexdump.print(4, this.spriteScanline);
				while (OAMEntry < 64 && spriteCount < 9) {

					int diff = scanline - OAM[OAMEntry * 4 + 0];

					if (diff >= 0 && diff < spriteSize) {

						if (spriteCount < 8) {
							spriteZeroHitPossible = OAMEntry == 0;
							spriteScanline[spriteCount * 4 + 0] = OAM[OAMEntry * 4 + 0];
							spriteScanline[spriteCount * 4 + 1] = OAM[OAMEntry * 4 + 1];
							spriteScanline[spriteCount * 4 + 2] = OAM[OAMEntry * 4 + 2];
							spriteScanline[spriteCount * 4 + 3] = OAM[OAMEntry * 4 + 3];
							spriteCount++;
						}

					}
					OAMEntry++;
				}
				// if (spriteCount > 3)
				// Hexdump.print(4, this.spriteScanline);
				status.setSpriteOveflow(spriteCount > 8);
			}

			if (cycle == 340) {

				for (int i = 0; i < spriteCount; i++) {
					int spritePatternBitsLo;
					int spritePatternBitsHi;
					int spritePatternAddrLo;
					int spritePatternAddrHi;

					if (control.spriteSize() == 0) {
						// 8x8
						// check the attribute byte.
						if ((spriteScanline[i * 4 + 2] & 0x80) == 0) {
							// normal flip horizontal
							spritePatternAddrLo = (control.patternSprite() << 12) | (spriteScanline[i * 4 + 1] << 4)
									| (scanline - spriteScanline[i * 4 + 0]);
						} else {
							// flip vertically
							spritePatternAddrLo = (control.patternSprite() << 12) | (spriteScanline[i * 4 + 1] << 4)
									| (7 - (scanline - spriteScanline[i * 4 + 0]));
						}

					} else {
						// 8x16
						if ((spriteScanline[i * 4 + 2] & 0x80) == 0) {
							// normal
							if ((scanline - spriteScanline[i * 4 + 0]) < 8) {
								spritePatternAddrLo = ((spriteScanline[i * 4 + 1] & 0x01) << 12)
										| ((spriteScanline[i * 4 + 1] & 0xfe) << 4)
										| ((scanline - spriteScanline[i * 4 + 0]) & 0x07);
							} else {
								spritePatternAddrLo = ((spriteScanline[i * 4 + 1] & 0x01) << 12)
										| (((spriteScanline[i * 4 + 1] & 0xfe) + 1) << 4)
										| ((scanline - spriteScanline[i * 4 + 0]) & 0x07);
							}
						} else {
							// verticall flip
							if ((scanline - spriteScanline[i * 4 + 0]) < 8) {
								spritePatternAddrLo = ((spriteScanline[i * 4 + 1] & 0x01) << 12)
										| (((spriteScanline[i * 4 + 1] & 0xfe) + 1) << 4)
										| (7 - (scanline - spriteScanline[i * 4 + 0]) & 0x07);
							} else {
								spritePatternAddrLo = ((spriteScanline[i * 4 + 1] & 0x01) << 12)
										| ((spriteScanline[i * 4 + 1] & 0xfe) << 4)
										| (7 - (scanline - spriteScanline[i * 4 + 0]) & 0x07);
							}
						}
					} // END OF ADDR CONDITIONS

					spritePatternAddrHi = spritePatternAddrLo + 8;
					spritePatternBitsLo = ppuRead(spritePatternAddrLo, false);
					spritePatternBitsHi = ppuRead(spritePatternAddrHi, false);

					if ((spriteScanline[i * 4 + 2] & 0x40) == 0x40) {
						spritePatternBitsLo = flipByte(spritePatternBitsLo);
						spritePatternBitsHi = flipByte(spritePatternBitsHi);
					}

					spriteShifterPatternLo[i] = spritePatternBitsLo;
					spriteShifterPatternHi[i] = spritePatternBitsHi;
					// System.out.println("low byte: " +
					// Hexdump.printHexPadded(spritePatternBitsLo, 4));
					// System.out.println("low byte: " +
					// Hexdump.printHexPadded(spritePatternBitsHi, 4));
				} // END OF FOR LOOP
			}
		}

		if (scanline == 240) {

		}

		if (scanline >= 241 && scanline < 261) {
			if (scanline == 241 && cycle == 1) {

				status.setVerticalBlank(true);

				if (control.enableNMI() == 1) {
					nmi = true;
				}

			}
		}

		int bgPixel = 0x00;
		int bgPalette = 0x00;
		if (mask.renderBackground() == 1) {
			int bitMux = 0x8000 >> fineX;
			int pixel0 = (bgShifterPatternLO & bitMux) > 0 ? 1 : 0;
			int pixel1 = (bgShifterPatternHI & bitMux) > 0 ? 1 : 0;

			bgPixel = (pixel1 << 1) | pixel0;

			int bgPalette0 = (bgShifterAttributeLO & bitMux) > 0 ? 1 : 0;
			int bgPalette1 = (bgShifterAttributeHI & bitMux) > 0 ? 1 : 0;
			bgPalette = (bgPalette1 << 1) | bgPalette0;
		}

		int fgPixel = 0x00;
		int fgPalette = 0x00;
		int fgPriority = 0x00;

		if (mask.renderSprites() == 1) {

			spriteZeroBeingRendered = false;

			for (int i = 0; i < spriteCount; i++) {
				if (spriteScanline[i * 4 + 3] == 0) {
					int fgPixelLo = (spriteShifterPatternLo[i] & 0x80) > 0 ? 1 : 0;
					int fgPixelHi = (spriteShifterPatternHi[i] & 0x80) > 0 ? 1 : 0;
					fgPixel = (fgPixelHi << 1) | fgPixelLo;

					fgPalette = (spriteScanline[i * 4 + 2] & 0x03) + 0x04;
					fgPriority = ((spriteScanline[i * 4 + 2] & 0x20) == 0) ? 1 : 0;

					if (fgPixel != 0) {

						spriteZeroBeingRendered = (i == 0);

						break;
					}
				}
			}
		}

		int pixel = 0x00;
		int palette = 0x00;

		if (bgPixel == 0 && fgPixel == 0) {

			pixel = 0x00;
			palette = 0x00;

		} else if (bgPixel == 0 && fgPixel > 0) {

			pixel = fgPixel;
			palette = fgPalette;

		} else if (bgPixel > 0 && fgPixel == 0) {

			pixel = bgPixel;
			palette = bgPalette;

		} else if (bgPixel > 0 && fgPixel > 0) {

			if (fgPriority != 0) {

				pixel = fgPixel;
				palette = fgPalette;

			} else {

				pixel = bgPixel;
				palette = bgPalette;

			}

			if (spriteZeroBeingRendered && spriteZeroHitPossible) {
				if ((mask.renderBackground() & mask.renderSprites()) == 1) {
					if (~(mask.renderBackgroundLeft() | mask.renderSpritesLeft()) == 1) {
						if (cycle >= 9 && cycle < 258) {
							status.setSpriteZeroHit(true);
						}
					} else {
						if (cycle >= 1 && cycle < 258) {
							status.setSpriteZeroHit(true);
						}
					}
				}
			}

		}

		int x = cycle - 1;
		int y = scanline;
		screen.fillRect(x * pixelSize, y * pixelSize, pixelSize, pixelSize, getColorFromPalette(palette, pixel));

		cycle++;
		if (cycle >= 341) {

			cycle = 0;
			scanline++;
			scanlineComplete = true;

			if (scanline >= 261) {

				scanline = -1;
				frameCount++;
				setFrameComplete(true);
			}
		}

	}

	// O---------------------------#
	// | HELPER FUNCTIONS
	// O---------------------------#

	private void incrementScrollX() {
		if (mask.renderBackground() == 1 || mask.renderSprites() == 1) {
			if (loopyV.coarseX() == 31) {
				loopyV.coarseX(0);
				loopyV.nametableX(~loopyV.nametableX());
			} else {
				int t = loopyV.coarseX();
				t++;
				loopyV.coarseX(t);
			}
		}
	}

	private int flipByte(int b) {
		b = (b & 0xF0) >> 4 | (b & 0x0F) << 4;
		b = (b & 0xCC) >> 2 | (b & 0x33) << 2;
		b = (b & 0xAA) >> 1 | (b & 0x55) << 1;
		return b;
	}
	private void incrementScrollY() {
		if (mask.renderBackground() == 1 || mask.renderSprites() == 1) {
			int fineY = loopyV.fineY();
			if (fineY < 7) {
				fineY++;
				loopyV.fineY(fineY);
			} else {
				loopyV.fineY(0);

				// Check if we need to swap vertical nametable targets
				if (loopyV.coarseY() == 29) {
					// We do, so reset coarse y offset
					loopyV.coarseY(0);
					// And flip the target nametable bit
					int t = loopyV.nametableY();
					loopyV.nametableY(~t);
				} else if (loopyV.coarseY() == 31) {
					// In case the pointer is in the attribute memory, we
					// just wrap around the current nametable
					loopyV.coarseY(0);
				} else {
					// None of the above boundary/wrapping conditions apply
					// so just increment the coarse y offset
					int t = loopyV.coarseY();
					t++;
					loopyV.coarseY(t);
				}
			}
		}

	}

	private void transferAddressX() {
		if (mask.renderBackground() == 1 || mask.renderSprites() == 1) {
			loopyV.nametableX(loopyT.nametableX());
			loopyV.coarseX(loopyT.coarseX());

		}
	}

	private void transferAddressY() {
		if (mask.renderBackground() == 1 || mask.renderSprites() == 1) {

			loopyV.fineY(loopyT.fineY());
			loopyV.nametableY(loopyT.nametableY());
			loopyV.coarseY(loopyT.coarseY());
		}
	}

	private void loadBackgroundShifters() {
		bgShifterPatternLO = (bgShifterPatternLO & 0xff00) | bgNextTileLsb;
		bgShifterPatternHI = (bgShifterPatternHI & 0xff00) | bgNextTileMsb;

		bgShifterAttributeLO = (bgShifterAttributeLO & 0xff00) | (((bgNextTileAttbr & 0b01) == 1) ? 0xff : 0x00);
		bgShifterAttributeHI = (bgShifterAttributeHI & 0xff00) | (((bgNextTileAttbr & 0b10) == 0b10) ? 0xff : 0x00);
	}

	private void updateShifters() {
		if (mask.renderBackground() == 1) {

			// Shifting background tile pattern row
			bgShifterPatternLO <<= 1;
			bgShifterPatternHI <<= 1;

			// Shifting palette attributes by 1
			bgShifterAttributeLO <<= 1;
			bgShifterAttributeHI <<= 1;
		}

		if ((mask.renderSprites() == 1) && cycle >= 1 && cycle < 258) {
			for (int i = 0; i < spriteCount; i++) {
				if (spriteScanline[i * 4 + 3] > 0) {
					spriteScanline[i * 4 + 3]--;
				} else {
					spriteShifterPatternLo[i] <<= 1;
					spriteShifterPatternHi[i] <<= 1;
				}
			}
		}
	}

	// O---------------------------#
	// | GETTERS & SETTERS
	// O---------------------------#

	public int getTileId() {
		return bgNextTileId;
	}

	public Cartridge getCart() {
		return cart;
	}

	public int getFrameCount() {
		return frameCount;
	}

	public int getScanline() {
		return scanline;
	}

	public int getCycle() {
		return cycle;
	}

	public Sprite getScreen() {
		return screen;
	}

	public boolean isFrameComplete() {
		return frameComplete;
	}

	public void setFrameComplete(boolean frameComplete) {
		this.frameComplete = frameComplete;
	}

	public Sprite getPatternTblScreen(int i) {
		return patternTblScreen[i];
	}

	public Sprite drawPatternTblScreen(int i, int palette) {

		// Get the tiles
		for (int tileY = 0; tileY < 16; tileY++) {
			for (int tileX = 0; tileX < 16; tileX++) {
				int patternTableWidth = 256;
				int offset = tileY * patternTableWidth + tileX * 16;

				// Get the pixels
				for (int row = 0; row < 8; row++) {
					// First plane
					int tileLsb = ppuRead(i * 0x1000 + offset + row, false);
					// Second plane
					int tileMsb = ppuRead(i * 0x1000 + offset + row + 8, false);

					for (int col = 0; col < 8; col++) {
						int pixel = (tileLsb & 0x01) + (tileMsb & 0x01);
						tileLsb >>= 1;
						tileMsb >>= 1;

						patternTblScreen[i].fillRect((tileX * 8 + (7 - col)), (tileY * 8 + row), 1, 1,
								getColorFromPalette(palette, pixel));
					}
				}

			}
		}

		return patternTblScreen[i];
	}

	public Pixel getColorFromPalette(int palette, int pixel) {
		return colorPallet[ppuRead(0x3f00 + (palette << 2) + pixel, false) & 0x3f];

	}

	public Sprite getNameTableScreen() {
		return nameTableScreen;
	}

	public int[] getPatternMem(int i) {
		return patternTable[i];
	}

	public int[][] getNametable() {
		return nametable;
	}

	public int[] getPaletteRam() {
		return palletRam;
	}

	// •---------------------------------
	// | COLOR PALETTE
	// •---------------------------------
	private void createColorPallet() {
		colorPallet[0x00] = new Pixel(84, 84, 84);
		colorPallet[0x01] = new Pixel(0, 30, 116);
		colorPallet[0x02] = new Pixel(8, 16, 144);
		colorPallet[0x03] = new Pixel(48, 0, 136);
		colorPallet[0x04] = new Pixel(68, 0, 100);
		colorPallet[0x05] = new Pixel(92, 0, 48);
		colorPallet[0x06] = new Pixel(84, 4, 0);
		colorPallet[0x07] = new Pixel(60, 24, 0);
		colorPallet[0x08] = new Pixel(32, 42, 0);
		colorPallet[0x09] = new Pixel(8, 58, 0);
		colorPallet[0x0A] = new Pixel(0, 64, 0);
		colorPallet[0x0B] = new Pixel(0, 60, 0);
		colorPallet[0x0C] = new Pixel(0, 50, 60);
		colorPallet[0x0D] = new Pixel(0, 0, 0);
		colorPallet[0x0E] = new Pixel(0, 0, 0);
		colorPallet[0x0F] = new Pixel(0, 0, 0);

		colorPallet[0x10] = new Pixel(152, 150, 152);
		colorPallet[0x11] = new Pixel(8, 76, 196);
		colorPallet[0x12] = new Pixel(48, 50, 236);
		colorPallet[0x13] = new Pixel(92, 30, 228);
		colorPallet[0x14] = new Pixel(136, 20, 176);
		colorPallet[0x15] = new Pixel(160, 20, 100);
		colorPallet[0x16] = new Pixel(152, 34, 32);
		colorPallet[0x17] = new Pixel(120, 60, 0);
		colorPallet[0x18] = new Pixel(84, 90, 0);
		colorPallet[0x19] = new Pixel(40, 114, 0);
		colorPallet[0x1A] = new Pixel(8, 124, 0);
		colorPallet[0x1B] = new Pixel(0, 118, 40);
		colorPallet[0x1C] = new Pixel(0, 102, 120);
		colorPallet[0x1D] = new Pixel(0, 0, 0);
		colorPallet[0x1E] = new Pixel(0, 0, 0);
		colorPallet[0x1F] = new Pixel(0, 0, 0);

		colorPallet[0x20] = new Pixel(236, 238, 236);
		colorPallet[0x21] = new Pixel(76, 154, 236);
		colorPallet[0x22] = new Pixel(120, 124, 236);
		colorPallet[0x23] = new Pixel(176, 98, 236);
		colorPallet[0x24] = new Pixel(228, 84, 236);
		colorPallet[0x25] = new Pixel(236, 88, 180);
		colorPallet[0x26] = new Pixel(236, 106, 100);
		colorPallet[0x27] = new Pixel(212, 136, 32);
		colorPallet[0x28] = new Pixel(160, 170, 0);
		colorPallet[0x29] = new Pixel(116, 196, 0);
		colorPallet[0x2A] = new Pixel(76, 208, 32);
		colorPallet[0x2B] = new Pixel(56, 204, 108);
		colorPallet[0x2C] = new Pixel(56, 180, 204);
		colorPallet[0x2D] = new Pixel(60, 60, 60);
		colorPallet[0x2E] = new Pixel(0, 0, 0);
		colorPallet[0x2F] = new Pixel(0, 0, 0);

		colorPallet[0x30] = new Pixel(236, 238, 236);
		colorPallet[0x31] = new Pixel(168, 204, 236);
		colorPallet[0x32] = new Pixel(188, 188, 236);
		colorPallet[0x33] = new Pixel(212, 178, 236);
		colorPallet[0x34] = new Pixel(236, 174, 236);
		colorPallet[0x35] = new Pixel(236, 174, 212);
		colorPallet[0x36] = new Pixel(236, 180, 176);
		colorPallet[0x37] = new Pixel(228, 196, 144);
		colorPallet[0x38] = new Pixel(204, 210, 120);
		colorPallet[0x39] = new Pixel(180, 222, 120);
		colorPallet[0x3A] = new Pixel(168, 226, 144);
		colorPallet[0x3B] = new Pixel(152, 226, 180);
		colorPallet[0x3C] = new Pixel(160, 214, 228);
		colorPallet[0x3D] = new Pixel(160, 162, 160);
		colorPallet[0x3E] = new Pixel(0, 0, 0);
		colorPallet[0x3F] = new Pixel(0, 0, 0);

	}

	public int getLoopyV() {
		return loopyV.register;
	}

	public int getControlReg() {
		return control.register();
	}

	public void connect(Bus bus) {
		this.bus = bus;

	}
}
