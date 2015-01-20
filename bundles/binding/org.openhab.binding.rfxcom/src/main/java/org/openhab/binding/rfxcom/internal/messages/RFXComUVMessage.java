/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.rfxcom.internal.messages;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.rfxcom.RFXComValueSelector;
import org.openhab.binding.rfxcom.internal.RFXComException;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;

/**
 * RFXCOM data class for UV message.
 * 
 * @author Rich Robinson
 * @since 0.0.1
 */
public class RFXComUVMessage extends RFXComBaseMessage {

	public enum SubType {
		UNDEF(0),
		UVN128_138(1),
		UVN800(2),
		UNKNOWN(255);

		private final int subType;

		SubType(int subType) {
			this.subType = subType;
		}

		SubType(byte subType) {
			this.subType = subType;
		}

		public byte toByte() {
			return (byte) subType;
		}
	}

	public enum UVDescription {
		LOW(0),
		MEDIUM(1),
		HIGH(2),
		VERY_HIGH(3),
		DANGEROUS(4),
		
		UNKNOWN(255);

		private final int uvDescription;

		UVDescription(int uvDescription) {
			this.uvDescription = uvDescription;
		}

		UVDescription(byte uvDescription) {
			this.uvDescription = uvDescription;
		}

		public byte toByte() {
			return (byte) uvDescription;
		}
	}

	private final static List<RFXComValueSelector> supportedValueSelectors = Arrays
			.asList(RFXComValueSelector.RAW_DATA,
					RFXComValueSelector.SIGNAL_LEVEL,
					RFXComValueSelector.BATTERY_LEVEL,
					RFXComValueSelector.UV_LEVEL,
					RFXComValueSelector.UV_DESCRIPTION);

	public SubType subType = SubType.UVN800;
	public int sensorId = 0;
	public byte uvLevel = 0;
	public UVDescription uvDescription = UVDescription.LOW;
	public byte signalLevel = 0;
	public byte batteryLevel = 0;

	public  RFXComUVMessage() {
		packetType = PacketType.UV;
	}

	public RFXComUVMessage(byte[] data) {
		encodeMessage(data);
	}

	@Override
	public String toString() {
		String str = "";

		str += super.toString();
		str += "\n - Sub type = " + subType;
		str += "\n - Id = " + sensorId;
		str += "\n - UV level = " + uvLevel;
		str += "\n - UV Description = " + uvDescription;
		str += "\n - Signal level = " + signalLevel;
		str += "\n - Battery level = " + batteryLevel;

		return str;
	}

	@Override
	public void encodeMessage(byte[] data) {

		super.encodeMessage(data);

		try {
			subType = SubType.values()[super.subType];
		} catch (Exception e) {
			subType = SubType.UNKNOWN;
		}
		sensorId = (data[4] & 0xFF) << 8 | (data[5] & 0xFF);
		uvLevel = (byte) ((data[6] & 0xF0) >> 4);

		try {
			uvDescription = UVDescription.values()[(byte) (data[6] & 0x0F)];
		} catch (Exception e) {
			uvDescription = UVDescription.UNKNOWN;
		}

		signalLevel = (byte) ((data[9] & 0xF0) >> 4);
		batteryLevel = (byte) (data[9] & 0x0F);
	}

	@Override
	public byte[] decodeMessage() {
		byte[] data = new byte[10];

		data[0] = 0x09;								// 09
		data[1] = RFXComBaseMessage.PacketType.UV.toByte();			// 57
		data[2] = subType.toByte();						// 02
		data[3] = seqNbr;							// 1A
		data[4] = (byte) ((sensorId & 0xFF00) >> 8);				// 4F
		data[5] = (byte) (sensorId & 0x00FF);					// 00
		data[6] = (byte) (((uvLevel & 0x0F) << 4) | (uvDescription.toByte() & 0x0F));	// 00
		data[7] = 0x00; //uvLevel;							// 70
		data[8] = 0x00; //uvDescription.toByte();					// 00
		data[9] = (byte) (((signalLevel & 0x0F) << 4) | (batteryLevel & 0x0F));	// 89

		return data;
	}
	
	@Override
	public String generateDeviceId() {
		 return String.valueOf(sensorId);
	}

	@Override
	public State convertToState(RFXComValueSelector valueSelector)
			throws RFXComException {
		
		org.openhab.core.types.State state = UnDefType.UNDEF;

		if (valueSelector.getItemClass() == NumberItem.class) {

			if (valueSelector == RFXComValueSelector.SIGNAL_LEVEL) {

				state = new DecimalType(signalLevel);

			} else if (valueSelector == RFXComValueSelector.BATTERY_LEVEL) {

				state = new DecimalType(batteryLevel);

			} else if (valueSelector == RFXComValueSelector.UV_LEVEL) {

				state = new DecimalType(uvLevel);

			} else {
				throw new RFXComException("Can't convert "
						+ valueSelector + " to NumberItem");
			}

		} else if (valueSelector.getItemClass() == StringItem.class) {

			if (valueSelector == RFXComValueSelector.RAW_DATA) {

				state = new StringType(
						DatatypeConverter.printHexBinary(rawMessage));

			} else if (valueSelector == RFXComValueSelector.UV_DESCRIPTION) {

				state = new StringType(uvDescription.toString());

			} else {
				throw new RFXComException("Can't convert " + valueSelector + " to StringItem");
			}
		} else {

			throw new RFXComException("Can't convert " + valueSelector
					+ " to " + valueSelector.getItemClass());

		}

		return state;
	}

	@Override
	public void convertFromState(RFXComValueSelector valueSelector, String id,
			Object subType, Type type, byte seqNumber) throws RFXComException {
		
		throw new RFXComException("Not supported");
	}

	@Override
	public Object convertSubType(String subType) throws RFXComException {
		
		for (SubType s : SubType.values()) {
			if (s.toString().equals(subType)) {
				return s;
			}
		}
		
		throw new RFXComException("Unknown sub type " + subType);
	}
	
	@Override
	public List<RFXComValueSelector> getSupportedValueSelectors() throws RFXComException {
		return supportedValueSelectors;
	}

}
