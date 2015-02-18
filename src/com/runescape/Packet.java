package com.runescape;

public class Packet {

	public static final int RESET_ENTITY_ANIMATIONS = 5;
	public static final int READ_IGNORE_LIST = 6;
	public static final int SET_DISABLED_WIDGET_MESSAGE = 9;
	public static final int ADD_LOCATION = 22;
	public static final int SET_LOCAL_PLAYER_INDEX = 27;
	public static final int REMOVE_LOCATION = 35;
	public static final int SET_DISABLED_WIDGET_MODEL = 42;
	public static final int SAVE_LOCATION_MAPS = 44;
	public static final int LOAD_SCENE = 47;
	public static final int READ_SECONDARY_PACKETS = 48;
	public static final int SAVE_LANDSCAPE_MAPS = 51;
	public static final int START_SYSTEM_UPDATE_TIMER = 53;
	public static final int ADD_SPOT_ANIMATION = 59;
	public static final int ADD_PROJECTILE = 60;
	public static final int SET_WIDGET_VISIBILITY = 61;
	public static final int READ_WIDGET_INVENTORY = 68;
	public static final int SHOW_TRANSFER_INPUT = 75;
	public static final int CLEAR_WIDGETS_AND_INPUTS = 80;
	public static final int SET_JINGLE = 85;
	public static final int ADD_PRIVATE_OBJECT_STACK = 90;
	public static final int SET_TEMPORARY_POSITION = 95;
	public static final int UPDATE_SKILL = 98;
	public static final int UPDATE_NPCS = 100;
	public static final int SET_DISABLED_WIDGET_MODEL_TO_OBJECT = 107;
	public static final int NOTIFY_FRIEND_STATUS = 114;
	public static final int SET_CHATBOX_WIDGET = 116;
	public static final int SET_INTEGER_VARIABLE = 119;
	public static final int SET_WIDGET_MODEL_TO_PLAYER_HEAD = 123;
	public static final int SET_SIDEBAR_WIDGET = 124;
	public static final int PERSUADE_LOGOUT = 126;
	public static final int ADD_OBJECT_STACK = 127;
	public static final int SET_VIEWPORT_AND_SIDEBAR_WIDGETS = 138;
	public static final int SET_DISABLED_WIDGET_ANIMATION = 149;
	public static final int ATTACH_TEMPORARY_LOCATION_TO_PLAYER = 153;
	public static final int ADD_MESSAGE = 156;
	public static final int SET_MIDI = 159;
	public static final int SET_VIEWPORT_WIDGET = 171;
	public static final int ADD_ANIMATED_LOCATION = 175;
	public static final int SET_WIDGET_MODEL_TO_NPC_HEAD = 181;
	public static final int CLEAR_8X8_OBJECTS_LOCS = 188;
	public static final int READ_LANDSCAPE_MAPS = 197;
	public static final int CLEAR_WIDGET_INVENTORY = 210;
	public static final int SET_MULTIZONE = 217;
	public static final int SET_CHAT_SETTINGS = 220;
	public static final int READ_LOCATION_MAPS = 225;
	public static final int SET_WIDGET_INVENTORY = 227;
	public static final int ADD_PRIVATE_MESSAGE = 229;
	public static final int LOAD_MAPS = 232;
	public static final int SET_TAB_WIDGET = 234;
	public static final int SET_SHORT_VARIABLE = 235;
	public static final int SET_DISABLED_WIDGET_COLOR = 248;
	public static final int REMOVE_OBJECT_STACK = 250;
	public static final int SET_SELECTED_TAB = 254;

	// thank u formatter
	public static final int[] SIZE = {
		//	0	1	2	3	4	5	6	7	8	9
		0, 0, 0, 0, 0, 0, -2, 0, 0, -2, //	0
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //	10
		0, 0, 4, 0, 0, 0, 0, 2, 0, 0, //	20
		0, 0, 0, 0, 0, 2, 0, 0, 0, 0, //	30
		0, 0, 4, 0, 2, 0, 0, -2, -2, 0, //	40
		0, 2, 0, 2, 0, 0, 0, 0, 0, 6, //	50
		15, 3, 0, 0, 0, 0, 0, 0, -2, 0, //	60
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //	70
		0, 0, 0, 0, 0, -1, 0, 0, 0, 0, //	80
		5, 0, 0, 0, 0, 2, 0, 0, 6, 0, //	90
		-2, 0, 0, 0, 0, 0, 0, 6, 0, 0, //	100
		0, 0, 0, 0, 9, 0, 2, 0, 0, 6, //	110
		0, 0, 0, 2, 2, 0, 0, 3, 0, 0, //	120
		0, 0, 0, 0, 0, 0, 0, 0, 4, 0, //	130
		0, 0, 0, 0, 0, 0, 0, 0, 0, 4, //	140
		0, 0, 0, 14, 0, 0, -1, 0, 0, -1, //	150
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //	160
		0, 2, 0, 0, 0, 4, 0, 0, 0, 0, //	170
		0, 4, 0, 0, 0, 0, 0, 0, 2, 0, //	180
		0, 0, 0, 0, 0, 0, 0, -2, 0, 0, //	190
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, //	200
		2, 0, 0, 0, 0, 0, 0, 1, 0, 0, //	210
		3, 0, 0, 0, 0, -2, 0, -2, 0, -1, //	220
		0, 0, -2, 0, 3, 3, 0, 0, 0, 0, //	230
		0, 0, 0, 0, 0, 0, 0, 0, 4, 0, //	240
		3, 0, 0, 0, 1, 0, 0 //	250
	//	0	1	2	3	4	5	6	7	8	9
	};

}
