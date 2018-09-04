package org.jarmoni.hsp_netty;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ByteUtilTest {

	@Test
	public void testSubArray() throws Exception {
		byte[] in = new byte[] { 0, 1, 2, 3, 4 };
		{
			byte[] out = ByteUtil.subArray(in, 2, 4);
			assertThat(out.length, is(2));
			assertThat(out[0], is((byte) 2));
			assertThat(out[1], is((byte) 3));
		}
		{
			byte[] out = ByteUtil.subArray(in, 2, 5);
			assertThat(out.length, is(3));
			assertThat(out[0], is((byte) 2));
			assertThat(out[1], is((byte) 3));
			assertThat(out[2], is((byte) 4));
		}
	}
}
