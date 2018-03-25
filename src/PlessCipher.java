import java.io.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class PlessCipher {
    public static final int REGISTERS_COUNT = 8;
    public static final int REGISTER_SIZE = 8;
    public static final int TRIGGER_COUNT = REGISTERS_COUNT / 2;

    private static ShiftRegister[] shiftRegisters = new ShiftRegister[REGISTERS_COUNT];
    private static JkTrigger[] jkTriggers = new JkTrigger[TRIGGER_COUNT];

    static {
        shiftRegisters[0] = new ShiftRegister(115, REGISTER_SIZE, new int[]{6, 2, 0, 5, 7});
        shiftRegisters[1] = new ShiftRegister(27, REGISTER_SIZE, new int[]{5, 2, 4, 1});
        shiftRegisters[2] = new ShiftRegister(201, REGISTER_SIZE, new int[]{7, 3, 2, 6});
        shiftRegisters[3] = new ShiftRegister(79, REGISTER_SIZE, new int[]{0, 4, 6, 2, 1});
        shiftRegisters[4] = new ShiftRegister(35, REGISTER_SIZE, new int[]{3, 7, 5});
        shiftRegisters[5] = new ShiftRegister(91, REGISTER_SIZE, new int[]{0, 1, 2, 5});
        shiftRegisters[6] = new ShiftRegister(56, REGISTER_SIZE, new int[]{4, 6, 1, 7});
        shiftRegisters[7] = new ShiftRegister(78, REGISTER_SIZE, new int[]{7, 5, 3, 4});

        for (int i = 0; i < TRIGGER_COUNT; i++) {
            jkTriggers[i] = new JkTrigger();
        }
    }


    static private int gammaBlock;
    static private int gammaBlockSize = 0;
    private static Random rand = new Random();

    static private int getNextGammaBit() {
        if (gammaBlockSize == 0) {
            for (int i = 0; i < REGISTERS_COUNT; i++) {
                shiftRegisters[i].shift();
                if (i % 2 == 0) {
                    jkTriggers[i / 2].setJ(shiftRegisters[i].getLowBit());
                } else {
                    jkTriggers[i / 2].setK(shiftRegisters[i].getLowBit());
                }
            }

            int mask = 0x01;
            gammaBlock = 0;
            for (int i = 0; i < TRIGGER_COUNT; i++, mask <<= 1) {
                boolean v = jkTriggers[i].getState();
                if (v) {
                    gammaBlock |= mask;
                }
            }
            gammaBlockSize = TRIGGER_COUNT;
        }
        int result = gammaBlock & 0x01;
        gammaBlock >>>= 1;
        gammaBlockSize--;
       return result;

        //return rand.nextInt(2);
    }

    static private int getNextGammaByte() {
        int result = 0;
        int mask = 0x080;
        for (int i = 0; i < 8; i++, mask >>>= 1) {
            int bit = getNextGammaBit();
            if (bit != 0) {
                result |= mask;
            }
        }
        return result & 0xff;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: PlessCipher.jar inputFile outputFile");
            System.out.println("Usage: PlessCipher.jar -runTests bitsCount");
            return;
        }
        if(args[0].equals("-runTests")) {
            int bitsCount = Integer.parseInt(args[1]);
            int bytesCnt = (int)Math.ceil(bitsCount / 8.);
            ByteBuffer buffer = ByteBuffer.allocate(bytesCnt);
            for (int i = 0; i < bytesCnt; i++) {
                buffer.put((byte) getNextGammaByte());
            }
            byte[] bitSequence = buffer.array();
            StatisticsTests.runTests(bitSequence, bitsCount);
        }else {
            encryptOrDecrypt(args[0], args[1]);
        }
    }

    private static void encryptOrDecrypt(String inpFile, String outFile) throws IOException {
        try (InputStream is = new FileInputStream(inpFile); OutputStream out = new FileOutputStream(outFile)) {
            int b;
            while ((b = is.read()) != -1) {
                b ^= getNextGammaByte();
                System.out.write((byte)b);
                out.write((byte)b);
            }
            out.flush();
            System.out.flush();
        }
    }
}
