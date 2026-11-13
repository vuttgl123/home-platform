package com.home_platform.registry.support;

/**
 * Mã geohash chuẩn (base32): chia đôi lần lượt kinh độ và vĩ độ, mỗi 5 bit thành một ký tự.
 * Hai điểm gần nhau thường có chung phần đầu mã, nên tìm quanh một toạ độ là quét index theo
 * tiền tố.
 */
public final class Geohash {

	private static final char[] BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray();

	private Geohash() {
	}

	public static String encode(double latitude, double longitude, int precision) {
		double[] lat = { -90, 90 };
		double[] lng = { -180, 180 };
		StringBuilder hash = new StringBuilder(precision);
		boolean evenBit = true;
		int bit = 0;
		int ch = 0;
		while (hash.length() < precision) {
			double[] range = evenBit ? lng : lat;
			double value = evenBit ? longitude : latitude;
			double mid = (range[0] + range[1]) / 2;
			if (value >= mid) {
				ch = (ch << 1) | 1;
				range[0] = mid;
			}
			else {
				ch = ch << 1;
				range[1] = mid;
			}
			evenBit = !evenBit;
			if (++bit == 5) {
				hash.append(BASE32[ch]);
				bit = 0;
				ch = 0;
			}
		}
		return hash.toString();
	}

}
