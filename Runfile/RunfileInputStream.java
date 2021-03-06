package IPNS.Runfile;


import java.io.InputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
/**
 *
 *
 * $Log$
 * Revision 1.1  2003/03/30 04:03:27  hammonds
 * New Class to handle the details of differences in ENDIAN and FLOATS in Runfiles.  This subclasses DataInputStream and uses the run version number to determine the structure of the data.
 *
 */

/** 
    This class is meant to handle the differnces in byte ordering between various
    runfile versions.  These differences mainly come from the fact that a VAX was
    used for the first few years of IPNS operation.  The VAX was a little endian
    machine and had it's own floating point format.  A transition to a new system
    which used Motorola processor boards and Java code which both support big
    endian data and IEEE floating point format.  Differences handled by this code
    use a version number stored in the run file to switch read methods.  By
    version these differences are:<p>
    <UL>
    <LI>Version <= 2 : data is little endian, neutron data is stored 2 bytes per
    channel, run numbers and calib file numbers are stored as 4 characters
    <LI>Version = 3 : data is little endian, neutron data is stored 2 bytes per
    channel, run numbers are stored as 4byte int's
    <LI>Version = 4 : data is little endian, neutron data is stored 4 bytes per
    channel, run numbers are stored as 4byte ints's
    <LI>Version >= 5: data is big endian, neutron data is stored 4 bytes per 
    channel, run numbers are stored as 4byte int's
    </UL>
    @author  John P. Hammonds
    @version 6.0

*/
public class RunfileInputStream extends DataInputStream {
  int version;     /** version number as read from the run file */
  
  /**
     Constructor method.  This method overloads the standard constructor method.  It
     runs the Constructor of the parent class and then reads the version number 
     from the run file.
   */

  protected RunfileInputStream ( InputStream is, int vers ) 
    throws IOException {
    super(is);

    version = vers;
    
  }

  /**
     Overload method for readInt() if version <3 assume little endian
   */
  public int readRunInt() throws IOException {
    int ret = 0;
    if (version <= 3) {
      int length = 4;
      int zero=0;
      byte b[] = new byte[length];
      int c[] = new int[length];
      int nBytesRead = read(b, zero, length);
      int num = zero;
      int i;
      int tfs = 256;
      c[0] = b[0];
      c[1] = b[1];
      if ( c[0] < zero) c[0] += tfs;
      if ( c[1] < zero) c[1] += tfs;
      num += c[0];
      num += (c[1] << 8);
      c[2] = b[2];
      c[3] = b[3];
      if ( c[2] < zero) c[2] += tfs;
      if ( c[3] < zero) c[3] += tfs;
      num += (c[2] << 16);
      num += (c[3] << 24);
      b = null;
      c = null;
      ret = num;
    }
    else {
      ret = readInt();
    }
    return ret;
  }

  /**
     Overload method for readShort() if version < 3 assume little endian
   */
  public short readRunShort() throws IOException {
    short ret = 0;
    if (version <= 3) {
      int length = 2;
      short zero=0;
      byte b[] = new byte[length];
      int c[] = new int[length];
      int nBytesRead = read(b, zero, length);
      short num = zero;
      int i;
      short tfs = 256;
      c[0] = b[0];
      c[1] = b[1];
      if ( c[0] < zero) c[0] += tfs;
      if ( c[1] < zero) c[1] += tfs;
      num += c[0];
      num += (c[1] << 8);
      b = null;
      c = null;
      ret = num;
    }
    else {
      ret = readShort();
    }
    return ret;
  }

  /**
     Overload method for readFloat()
   */
  public float readRunFloat() throws IOException {
    float ret = 0.0f;
    if (version <= 3) {
        int length = 4;
        long hi_mant, low_mant, exp, sign;
        double f_val;
        long val = (long )readRunInt();
        if (val < 0) {
	    val = val + 4294967296L;
	    /*	    val = val + (long)Math.pow(2.0, 32.0);*/
	}
	/* add 128 to put in the implied 1 */
        hi_mant  = (val & 127) + 128;
        val      = val >> 7;
	/* exponent is "excess 128" */
        exp      = ((int)(val & 255)) - 128;
        val      = val >> 8;

        sign     = val & 1;
        low_mant = val >> 1;
	/* This could also be a "reserved" operand of some sort?*/
        if ( exp == -128 )
	    f_val = 0;
        else
	    f_val = ((hi_mant /256.0) + (low_mant/16777216.0)) *
		Math.pow(2.0, (double)exp );

        if ( sign == 1 )
	    f_val = -f_val;
        ret = (float)f_val;
    }
    else {
      ret = readFloat();
    }
    return ret;
  }

  /**
     Read a String numChars long from starting at the next character in the file
   */
  public String readRunString(int numChar) throws IOException {
    byte[] temp = new byte[numChar];
    read(temp, 0, numChar);
    return new String(temp);
  }


  /**
     Read a file number from the header and return as an integer.  In older
     files ( some version 2) this was stored as 4 characters, newer files 
     (some version 2, and >3 these were stored a 4byte ints.
   */
  public int readRunFileNum() throws IOException {
    int ret = 0;  
    if (version <= 3) {
      byte[] temp = new byte[4];
      read(temp, 0, 4);
      boolean old = true;
      for (int ii = 0; ii < 4; ii++){
	if ( temp[ii] < '0' || temp[ii] > '9' ) old = false;

      }
      if ( old ) {
	ret = (new Integer(new String(temp))).intValue();
      }
      else {
	int temp2 = 0;
	temp2 = temp[0];
	if ( temp2 < 0 ) temp2 += 256;
	ret += temp2;
	temp2 = temp[1];
	if ( temp2 < 0 ) temp2 += 256;
	ret += (temp2 << 8);
	temp2 = temp[2];
	if ( temp2 < 0 ) temp2 += 256;
	ret += (temp2 << 16);
	temp2 = temp[3];
	if ( temp2 < 0 ) temp2 += 256;
	ret += (temp2 << 24);
      }
    }
    else if (version > 3) {
      ret =  readRunInt();
    }
    return ret;
  }

  /**
   */
  public int Version(){
    return version;
  }

  /**
   */
  /*  public static void main( String[] args ){
    try {
      RunfileInputStream runFile = new RunfileInputStream( args[0], "r" );
      int vers = runFile.Version();
      System.out.println(args[0] + " is version " + vers);
	}
    catch (IOException ex) {
      System.out.println("Problem opening file: " + args[0]);
    }
  }
  */ 
}
