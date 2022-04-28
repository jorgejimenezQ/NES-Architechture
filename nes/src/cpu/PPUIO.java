package cpu;

/**
 * 
 * TODO: https://wiki.nesdev.com/w/index.php/PPU_registers#PPUMASK
 * 
 * @author jorgejimenez
 *
 */
public class PPUIO {

	public static class Status {
		private int register = 0;
		private boolean spriteOveflow = false;
		private boolean spriteZeroHit = false;
		private boolean verticalBlank = false;

		public void set(int data) {

			if (data == 0) {
				reset();
				return;
			}
			spriteOveflow = getBitBool(5, data);
			spriteZeroHit = getBitBool(6, data);
			verticalBlank = getBitBool(7, data);
			register = data;
		}

		private void reset() {
			spriteOveflow = false;
			spriteZeroHit = false;
			verticalBlank = false;
			register = 0;
		}

		public int register() {
			return register;
		}

		public int spriteOveflow() {
			return spriteOveflow ? 1 : 0;
		}

		public int spriteZeroHit() {
			return spriteZeroHit ? 1 : 0;
		}

		public int verticalBlank() {
			return verticalBlank ? 1 : 0;
		}

		public void setSpriteOveflow(boolean spriteOveflow) {
			this.spriteOveflow = spriteOveflow;
			register = spriteOveflow ? setBit(register, 5) : clearBit(register, 5);
		}

		public void setSpriteZeroHit(boolean spriteZeroHit) {
			this.spriteZeroHit = spriteZeroHit;
			register = spriteZeroHit ? setBit(register, 6) : clearBit(register, 6);
		}

		public void setVerticalBlank(boolean verticalBlank) {
			this.verticalBlank = verticalBlank;
			register = verticalBlank ? setBit(register, 7) : clearBit(register, 7);
		}

	}

	public static class Mask {
		private int register = 0;
		private boolean grayscale = false;
		private boolean renderBackgroundLeft = false;
		private boolean renderSpritesLeft = false;
		private boolean renderBackground = false;
		private boolean renderSprites = false;
		private boolean emphasizeRed = false;
		private boolean emphasizeGreen = false;
		private boolean emphasizeBlue = false;

		public void set(int data) {

			if (data == 0) {
				reset();
				return;
			}

			grayscale = getBitBool(0, data);
			renderBackgroundLeft = getBitBool(1, data);
			renderSpritesLeft = getBitBool(2, data);
			renderBackground = getBitBool(3, data);
			renderSprites = getBitBool(4, data);
			emphasizeRed = getBitBool(5, data);
			emphasizeGreen = getBitBool(6, data);
			emphasizeBlue = getBitBool(7, data);

			register = data;
		}

		private void reset() {
			grayscale = false;
			renderBackgroundLeft = false;
			renderSpritesLeft = false;
			renderBackground = false;
			renderSprites = false;
			emphasizeRed = false;
			emphasizeGreen = false;
			emphasizeBlue = false;
			register = 0;
		}

		public int register() {
			return register;
		}

		public int grayscale() {
			return grayscale ? 1 : 0;
		}

		public int renderBackgroundLeft() {
			return renderBackgroundLeft ? 1 : 0;
		}

		public int renderSpritesLeft() {
			return renderSpritesLeft ? 1 : 0;
		}

		public int renderBackground() {
			return renderBackground ? 1 : 0;
		}

		public int renderSprites() {
			return renderSprites ? 1 : 0;
		}

		public int emphasizeRed() {
			return emphasizeRed ? 1 : 0;
		}

		public int emphasizeGreen() {
			return emphasizeGreen ? 1 : 0;
		}

		public int emphasizeBlue() {
			return emphasizeBlue ? 1 : 0;
		}

		public void setRegister(int register) {
			this.register = register;
		}

		public void setGrayscale(boolean grayscale) {
			this.grayscale = grayscale;
			register = grayscale ? setBit(register, 0) : clearBit(register, 0);
		}

		public void setRenderBackgroundLeft(boolean renderBackgroundLeft) {
			this.renderBackgroundLeft = renderBackgroundLeft;
			register = renderBackgroundLeft ? setBit(register, 1) : clearBit(register, 1);
		}

		public void setRenderSpritesLeft(boolean renderSpritesLeft) {
			this.renderSpritesLeft = renderSpritesLeft;
			register = renderSpritesLeft ? setBit(register, 2) : clearBit(register, 2);
		}

		public void setRenderBackground(boolean renderBackground) {
			this.renderBackground = renderBackground;
			register = renderBackground ? setBit(register, 3) : clearBit(register, 3);
		}

		public void setRenderSprites(boolean renderSprites) {
			this.renderSprites = renderSprites;
			register = renderSprites ? setBit(register, 4) : clearBit(register, 4);
		}

		public void setEmphasizeRed(boolean emphasizeRed) {
			this.emphasizeRed = emphasizeRed;
			register = emphasizeRed ? setBit(register, 5) : clearBit(register, 5);
		}

		public void setEmphasizeGreen(boolean emphasizeGreen) {
			this.emphasizeGreen = emphasizeGreen;
			register = emphasizeGreen ? setBit(register, 6) : clearBit(register, 6);
		}

		public void setEmphasizeBlue(boolean emphasizeBlue) {
			this.emphasizeBlue = emphasizeBlue;
			register = emphasizeBlue ? setBit(register, 7) : clearBit(register, 7);
		}
	}

	public static class Controller {
		private int register = 0;
		private boolean nametableX = false;
		private boolean nametableY = false;
		private boolean incrementMode = false;
		private boolean patternSprite = false;
		private boolean patternBackground = false;
		private boolean spriteSize = false;
		private boolean enableNMI = false;

		public void set(int data) {

			if (data == 0) {
				reset();
				return;
			}

			nametableX = getBitBool(0, data);
			nametableY = getBitBool(1, data);
			incrementMode = getBitBool(2, data);
			patternSprite = getBitBool(3, data);
			patternBackground = getBitBool(4, data);
			spriteSize = getBitBool(5, data);
			enableNMI = getBitBool(7, data);

			register = data;
		}

		public void add(int data) {

		}

		private void reset() {
			nametableX = false;
			nametableY = false;
			incrementMode = false;
			patternSprite = false;
			patternBackground = false;
			spriteSize = false;
			enableNMI = false;
			register = 0;

		}

		public int register() {
			return register;
		}

		public int nametableX() {
			return nametableX ? 1 : 0;
		}

		public int nametableY() {
			return nametableY ? 1 : 0;
		}

		public int incrementMode() {
			return incrementMode ? 1 : 0;
		}

		public int patternSprite() {
			return patternSprite ? 1 : 0;
		}

		public int patternBackground() {
			return patternBackground ? 1 : 0;
		}

		public int spriteSize() {
			return spriteSize ? 1 : 0;
		}

		public int enableNMI() {
			return enableNMI ? 1 : 0;
		}

		public void setRegister(int register) {
			this.register = register;
		}

		public void setNametableX(boolean nametableX) {
			this.nametableX = nametableX;
			register = nametableX ? setBit(register, 0) : clearBit(register, 0);
		}

		public void setNametableY(boolean nametableY) {
			this.nametableY = nametableY;
			register = nametableY ? setBit(register, 1) : clearBit(register, 1);
		}

		public void setIncrementMode(boolean incrementMode) {
			this.incrementMode = incrementMode;
			register = incrementMode ? setBit(register, 2) : clearBit(register, 2);
		}

		public void setPatternSprite(boolean patternSprite) {
			this.patternSprite = patternSprite;
			register = patternSprite ? setBit(register, 3) : clearBit(register, 3);
		}

		public void setPatternBackground(boolean patternBackground) {
			this.patternBackground = patternBackground;
			register = patternBackground ? setBit(register, 4) : clearBit(register, 4);
		}

		public void setSpriteSize(boolean spriteSize) {
			this.spriteSize = spriteSize;
			register = spriteSize ? setBit(register, 5) : clearBit(register, 5);
		}

		public void setEnableNMI(boolean enableNMI) {
			this.enableNMI = enableNMI;
			register = enableNMI ? setBit(register, 7) : clearBit(register, 7);
		}
	}

	public static class loopyReg {
		public int register = 0;

		public void register(int data) {
			register = data;
		}

		public int register() {
			return register;
		}

		public void add(int data) {
			register += data;
		}

		public int coarseX() {
			int coarseX = 0;

			for (int i = 0; i < 5; i++) {
				coarseX |= (getBit(i, register) << i);
			}

			return coarseX;
		}

		public void coarseX(int data) {

			register &= ~0b11111;
			if (data == 0) {
				return;
			}

			data &= 0x1f;
			int c = 0;

			for (int i = 0; i < 5; i++) {

				c = (getBitBool(i, data)) ? setBit(c, i) : clearBit(c, i);

			}

			register |= c;
		}

		public int coarseY() {
			int coarseY = 0;

			for (int i = 5; i < 10; i++) {
				coarseY |= (getBit(i, register) << i - 5);
			}

			return coarseY;
		}

		public void coarseY(int data) {
			register &= ~(0b11111 << 5);
			if (data == 0) {
				return;
			}

			data &= 0x1f;
			int c = 0;

			for (int i = 0; i < 5; i++) {

				c = (getBitBool(i, data)) ? setBit(c, i + 5) : clearBit(c, i + 5);

			}

			register |= c;
		}

		public int nametableX() {
			return getBit(10, register);
		}

		public void nametableX(int data) {
			data &= 1;
			register = (data == 1) ? setBit(register, 10) : clearBit(register, 10);
		}

		public int nametableY() {
			return getBit(11, register);
		}

		public void nametableY(int data) {
			data &= 1;

			register = (data == 1) ? setBit(register, 11) : clearBit(register, 11);
		}

		public void fineY(int data) {
			register = (getBitBool(0, data)) ? setBit(register, 12) : clearBit(register, 12);
			register = (getBitBool(1, data)) ? setBit(register, 13) : clearBit(register, 13);
			register = (getBitBool(2, data)) ? setBit(register, 14) : clearBit(register, 14);
		}

		public int fineY() {
			int fineY = 0;
			fineY |= getBit(12, register) << 0;
			fineY |= getBit(13, register) << 1;
			fineY |= getBit(14, register) << 2;
			return fineY;
		}

		@Override
		public String toString() {
			return Integer.toString(register);
		}
	}

	// •---------------------------
	// | HELPERS
	// •---------------------------

	private static boolean getBitBool(int position, int value) {
		return ((value >> position) & 1) == 1;
	}

	private static int getBit(int position, int value) {
		return (value >> position) & 1;
	}

	private static int setBit(int value, int position) {
		value |= 1 << position;
		return value;
	}

	private static int clearBit(int value, int position) {
		value &= ~(1 << position);
		return value;

	}

	public static void main(String[] arg) {
		loopyReg v = new loopyReg();

//		v.register(0b1110_1011_1111);
//
//		System.out.println((~0b11111));
//		System.out.println(v.register & (~0b11111));
//		System.out.println(v.register & ~(((1 << (6 - 1)) - 1) ^ ((1 << (10)) - 1)));
		v.register(0b10_00000_00010);

		System.out.println(v);
		printBinary(v.register);

//		v.fineY(0b100);

//		printBinary(v.register);
		printBinary(v.fineY());
		System.out.println(v.fineY());
//		System.out.println(v.nametableX());
//		System.out.println(v.nametableY());

	}

	private static void printBinary(int i) {
		System.out.println(Integer.toBinaryString(i));

	}
}