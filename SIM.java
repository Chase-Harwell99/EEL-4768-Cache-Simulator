// Chase Harwell
// 4614181
// EEL 4768 Fall 2021

import java.util.*;
import java.io.*;
import java.lang.*;

public class SIM
{
	private final double logBase = Math.log(2);
	private int[] binaryNum = new int[64];
	private long hits = 0;
	private long misses = 0;
	private long memoryReads = 0;
	private long memoryWrites = 0;
	private static final String[] binaryVals = {"0000", "0001", "0010", "0011",
												"0100", "0101", "0110", "0111",
												"1000", "1001", "1010", "1011",
												"1100", "1101", "1110", "1111"};

	public SIM(String cacheSize, String associate, String replacement, String write, String trace)
	{

		// Turns cache size and associativity command line arugments into workable integers
		int size = Integer.parseInt(cacheSize);
		int associativity = Integer.parseInt(associate);

		// Determines the number of sets, index bits, offset bits, and tag bits
		int indexBits = (int)((Math.log(size) / logBase) - (Math.log(associativity) / logBase) - 6);
		int sets = (int)Math.pow(2, indexBits);
		int offsetBits = 6; // 6 since block size is constantly 64B
		int tagBits = 64 - indexBits - offsetBits;

		// Initialize all of the used arrays
		String[][] tag_array = new String[sets][associativity];
		int[] fifo_array = new int[sets];
		boolean[][] dirty = new boolean[sets][associativity];

		// Find the input file and throw an exception if it doesn't exist
		File input;
		Scanner in;
		try {
			input  = new File(trace);
			in = new Scanner(input);
		} catch (FileNotFoundException e) {
			System.out.println("Error: File does not exist!");
			return;
		}

		// Main loop
		while(in.hasNext())
		{
			// Reads in if read/write and the address
			String mode = in.next();
			String address = in.next();

			// Remove 0x from the beginning of the address string
			address = address.substring(1, address.length());
			address = address.substring(1, address.length());

			// Convert the address to a binary number using an int array of size 64
			binaryNum = hexToBinary(address);

			// Make the binary array for the set number
			int[] set = new int[indexBits];
			for (int i = 0; i < indexBits; i++)
			{
				set[i] = binaryNum[58-indexBits+i];
			}

			// Convert the set number integer array to an integer
			int setNum = (int)binaryToDecimal(set);

			// Create an array of tag data and insert the tag bits into it
			int[] tagArray = new int[tagBits];
			for (int i  = 0; i < tagBits; i++)
			{
				tagArray[i] = binaryNum[i];
			}

			// Convert the binary array into a tag String to be inserted into the cache
			String tag = binaryToHex(tagArray, tagBits);

			int location = 0;
			boolean found = false;

			// Writing to cache using write through means writing to memory and cache
			if (mode.equals("W") && write.equals("0"))
			{
				memoryWrites++;
			}

			// Check to see if the tag exists in the array at that set number
			for (int i = 0; i < associativity; i++)
			{
				// Array at set number isn't full so no evictions will be neccessary
				if (tag_array[setNum][i] == null)
				{
					break;
				}

				// Tag found in the array
				if (tag_array[setNum][i].equals(tag))
				{
					hits++;
					found = true;
					location = i;
					break;
				}
			}

			// Add a miss and memory access if the data was not found in cache
			if (found == false)
			{
				misses++;
				memoryReads++;
			}

			// Replacement policy: LRU
			if (replacement.equals("0"))
			{
				boolean hitDirtyBit = false;

				// Data was found, so it needs to be brought to the front of the cache
				if (found)
				{
					hitDirtyBit = dirty[setNum][location];
					for (int i = location; i > 0; i--)
					{
						tag_array[setNum][i] = tag_array[setNum][i-1];
						dirty[setNum][i] = dirty[setNum][i-1];
					}
				}
				// Data was not found, if the data is dirty then it needs to be evicted and written to memory
				else
				{

					// Something is getting evicted. Write it to memory
					if (dirty[setNum][associativity-1] == true)
					{
						memoryWrites++;
					}

					// Move everything down by one in the array
					for (int i = associativity-1; i > 0; i--)
					{
						tag_array[setNum][i] = tag_array[setNum][i-1];
						dirty[setNum][i] = dirty[setNum][i-1];
					}
				}

				// Insert the new piece of data
				tag_array[setNum][0] = tag;
				if (mode.equals("W") && write.equals("1"))
				{
					dirty[setNum][0] = true;
				}
				else
				{
					dirty[setNum][0] = hitDirtyBit;
				}
			}

			// Replacement policy: FIFO
			else if (replacement.equals("1"))
			{
				// If it is found then nothing changes in fifo order
				// Only replace if the request is a miss
				if (!found)
				{
					// Something is getting evicted. Write it to memory
					if (dirty[setNum][associativity-1] == true)
					{
						memoryWrites++;
					}

					// Put the tag string in the last inserted space in the array
					tag_array[setNum][fifo_array[setNum]] = tag;

					// Set the bit dirty if neccessary
					if (mode.equals("W") && write.equals("1"))
					{
						dirty[setNum][fifo_array[setNum]] = true;
					}
					else
					{
						dirty[setNum][fifo_array[setNum]] = false;
					}

					// Increment the array index
					fifo_array[setNum]++;

					// Set array index back to 0 if end of the array has been reached
					if (fifo_array[setNum] == associativity)
					{
						fifo_array[setNum] = 0;
					}
				}
			}
		}

		// Retrieve and print all of the stats from the cache simulation
		long total = misses + hits;
		float fl_misses = (float)misses;
		float fl_total = (float)total;
		float missRatio = (fl_misses / fl_total);

		System.out.print("Miss Rate: ");
		System.out.printf("%.6f", missRatio);
		System.out.println();

		System.out.println("Memory Writes: " + memoryWrites);
		System.out.println("Memory Reads: " + memoryReads);

	}

	// Take in a hexadecimal String and convert it to a binary integer array to be returned
	public static int[] hexToBinary(String hex)
	{

		int[] returnArray = new int[64];
		String referenceHex = "0123456789ABCDEF";

		hex = hex.toUpperCase();
		int index = 60;
		for (int i = hex.length()-1; i >= 0; i--)
		{
			char val = hex.charAt(i);
			int number = referenceHex.indexOf(val);
			String binarydigit = binaryVals[number];
			int[] numberarray = new int[4];
			for (int k = 0; k < 4; k++)
			{
				char value = binarydigit.charAt(k);
				numberarray[k] = (int)value - 48;
			}

			for (int j = 0; j < 4; j++)
			{
				returnArray[index+j] = numberarray[j];
			}
			index -= 4;
		}
		return returnArray;
	}

	// Take in a binary integer array and convert it to a long decimal value
	public static long binaryToDecimal(int[] array)
	{
		long result = 0;
		int count = 0;
		for (int i = array.length - 1; i >= 0; i--)
		{
			if (array[i] == 1)
			{
				result += Math.pow(2, count);
			}
			count++;
		}
		return result;
	}

	// Take in the binary array and convert it back to a hexadecimal string
	// Used to get the tag string that will be stored in the cache blocks
	public static String binaryToHex(int[] binary, int tagSize)
	{
		long decimal_equivalent = binaryToDecimal(binary);
		String hexVal = Long.toHexString(decimal_equivalent);

		return hexVal;
	}

	public static void main(String[] args)
	{
		try
		{
			if (((args[2].equals("1")) || (args[2].equals("0"))) && ((args[3].equals("1")) || (args[3].equals("0"))))
			{
				SIM sim = new SIM(args[0], args[1], args[2], args[3], args[4]);
			}
			else
			{
				throw new Exception();
			}

		}
		catch (Exception e)
		{
			System.out.println("Error: Incorrect command line arguments. Please try again.");
		}
	}
}
