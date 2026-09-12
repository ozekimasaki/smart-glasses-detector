#!/usr/bin/env python3
"""
BLE Smart Glasses Emulator for Raspberry Pi
Usage: sudo python3 ble_smartglasses_emulator.py

スマートグラス検出アプリのテスト用。
ラズパイからスマートグラスのBLEアドバタイズを模擬送信します。
"""

import subprocess
import time
import sys

# アプリの Constants.kt に対応するメーカー一覧
COMPANY_ID_DEVICES = {
    1: ("Seiko Epson (Moverio)", 0x0040),
    2: ("Apple (Vision Pro)", 0x004C),
    3: ("Google (Glass)", 0x00E0),
    4: ("Amazon (Echo Frames)", 0x0171),
    5: ("Meta (Ray-Ban Meta)", 0x01AB),
    6: ("Meta (alternate)", 0x058E),
    7: ("Huawei (Eyewear)", 0x027D),
    8: ("Lenovo (Glasses)", 0x02C5),
    9: ("Meizu", 0x03AB),
    10: ("Snapchat (Spectacles)", 0x03C2),
    11: ("TCL (RayNeo)", 0x0BC6),
    12: ("Luxottica", 0x0D53),
    13: ("Vuzix", 0x060C),
    14: ("Kopin (Solos)", 0x041F),
    15: ("North (Focals)", 0x0562),
    16: ("Fauna", 0x0976),
    17: ("Even Realities (G1)", 0x10F9),
    18: ("Engo / ActiveLook", 0x08F2),
}

# デバイス名パターンで検出されるもの
NAME_PATTERN_DEVICES = {
    21: "XREAL Air 2",
    22: "Rokid Max",
    23: "INMO Air2",
    24: "Looktech 09",
    25: "LAWAKEN Glass",
    26: "Halliday glasses",
    27: "VITURE Pro",
    28: "Even G1-2048",
    29: "Brilliant Labs Frame",
    30: "HeyCyan-A1B2",
    31: "Apple Vision Pro",
    32: "Glass EE2",
    33: "AI Glasses-9C",
    34: "Halliday Glass",
    35: "Lucyd Lyte",
    36: "Mentra Live",
    37: "XyBLE_A1B2",
    38: "A.Look 000128",
    39: "Frame-1A2B",
    40: "Even G2_12_L_ABCDEF",
    41: "INMOAIR3_A1B2",
    42: "Galaxy XR-01",
    43: "Frame 4F",
    44: "HALLIDAYGP101",
    45: "Even G3_12_L",
    46: "智能眼镜-A1",
    47: "AR99",
    48: "Mentra Display",
    49: "Halo 4F",
    50: "G2_12_L",
    54: "Xy_A",
    55: "Nex1-77",
    56: "mentra_live_abc",
    57: "Nimo-A1B2",
    58: "LCD008-10",
    59: "Halliday G2",
    60: "Glasses_A1B2",
    62: "Solos AirGo3 1234",
    63: "Solos AirGo 3 1234",
    64: "RayNeo Air 4 Pro",
    65: "NexSim A1B2C3",
    66: "雷鸟Air 2",
    67: "若琪眼镜",
    68: "映莫GO2",
    69: "华为眼镜",
    70: "G1_12_L",
    75: "G1_12_R",
    76: "XREAL One Pro",
    77: "Oakley Meta",
    78: "VITURE Beast",
    79: "Rokid Max 2",
    80: "Echo Frames 2",
    82: "スマートメガネ",
    83: "RayNeo Air 3s",
    84: "XREAL One S",
    85: "VITURE Luma Pro",
    86: "Smart Glasses",
    87: "スマートグラス",
    89: "MemoMind One",
    90: "Dymesty Cook Edge",
    103: "小度AI眼镜",
    104: "Monako Glass",
    105: "讯飞AI眼镜",
    106: "夸克AI眼镜",
    107: "豆包AI眼镜",
    108: "Everysight Maverick",
    109: "OpenGlass",
    110: "XRAI Glass",
}

UUID_DEVICES = {
    51: ("Rokid Glasses (Service UUID 0x9100)", 0x9100),
    52: ("Snap Spectacles (Service UUID 0xFE45)", 0xFE45),
    53: ("Meta Ray-Ban (Service UUID 0xFD5F)", 0xFD5F),
}

UUID128_DEVICES = {
    71: ("Brilliant Labs Frame/Halo Lua", "7A230001-5475-A6A4-654C-8431F6AD49C4"),
    72: ("Engo ActiveLook GATT", "0783B03E-8535-B5A0-7140-A304D2495CB7"),
    73: ("HeyCyan / Nilox", "7905FFF0-B5CE-4E99-A40F-4B1E122D00D0"),
}

# メーカーデータ先頭の ASCII プロジェクト ID（MentraOS AR99 など）
PAYLOAD_DEVICES = {
    61: ("Xingyi AR99 (payload AR99)", "AR99"),
    74: ("Meta Ray-Ban payload", "META_RB_GLASS"),
}

APPEARANCE_DEVICES = {
    81: ("GAP Appearance eyeglasses", 0x01C0),
}

# 分類器がスマートグラスとして検出しない対照サンプル（ユニットテスト専用）
NON_GLASSES_NAME_DEVICES = {
    91: "Halo Band",
    92: "Quest 3",
    93: "Echo Dot",
    94: "AirPods Pro",
    95: "Galaxy Buds2",
    96: "Meta Band 00JT",
    97: "R1",
    98: "Even R1",
    99: "Even Realities R1",
    100: "Frame TV",
    101: "雷鸟TV",
    102: "华为手表",
}


def run(cmd):
    subprocess.run(cmd, shell=True, capture_output=True)


def stop_advertise():
    run("sudo hciconfig hci0 noleadv")
    print("  -> アドバタイズ停止")


def start_advertise_company_id(name, company_id, device_name="BLE Device"):
    stop_advertise()
    time.sleep(0.5)

    run("sudo hciconfig hci0 up")

    # デバイス名設定
    run(f'sudo hciconfig hci0 name "{device_name}"')

    # Manufacturer Specific Data (Type 0xFF) を含むアドバタイズデータ構築
    low = company_id & 0xFF
    high = (company_id >> 8) & 0xFF

    # ADV データ: Flags(3bytes) + Manufacturer Specific(5bytes)
    adv_data = (
        f"sudo hcitool -i hci0 cmd 0x08 0x0008 "
        f"0F "  # total length
        f"02 01 06 "  # Flags: General Discoverable + BR/EDR Not Supported
        f"05 FF {low:02X} {high:02X} 01 02 "  # Manufacturer Specific Data
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00 00"
    )
    run(adv_data)

    # アドバタイズ開始 (3 = non-connectable undirected)
    run("sudo hciconfig hci0 leadv 3")
    print(f"  -> 送信中: {name} (Company ID: 0x{company_id:04X}, Name: {device_name})")


def start_advertise_name_only(device_name):
    stop_advertise()
    time.sleep(0.5)

    run("sudo hciconfig hci0 up")
    run(f'sudo hciconfig hci0 name "{device_name}"')

    # Flags + Complete Local Name（日本語・中国語名は UTF-8。31バイトADVに収める）
    name_bytes = device_name.encode("utf-8")[: 31 - 5]
    name_len = len(name_bytes)
    name_hex = " ".join(f"{b:02X}" for b in name_bytes)

    # ADV data: Flags(3) + Name(2+name_len)
    total = 3 + 2 + name_len
    pad_len = 31 - total
    pad = " ".join(["00"] * pad_len) if pad_len > 0 else ""

    adv_data = (
        f"sudo hcitool -i hci0 cmd 0x08 0x0008 "
        f"{total:02X} "
        f"02 01 06 "  # Flags
        f"{name_len + 1:02X} 09 {name_hex} "  # Complete Local Name
        f"{pad}"
    )
    run(adv_data)
    run("sudo hciconfig hci0 leadv 3")
    print(f"  -> 送信中: デバイス名 = {device_name}")


def start_advertise_ascii_manufacturer_payload(label, ascii_id):
    stop_advertise()
    time.sleep(0.5)

    run("sudo hciconfig hci0 up")
    run('sudo hciconfig hci0 name "BLE Device"')

    ident = ascii_id.encode("ascii")
    if len(ident) <= 4:
        # MentraOS は manufacturer data が 20 バイト以上のとき project name を読む
        payload = ident.ljust(4, b"\x00") + bytes(16)
    else:
        payload = ident[:26]
    rec_len = 1 + len(payload)
    total = 3 + 1 + rec_len
    payload_hex = " ".join(f"{b:02X}" for b in payload)
    pad_len = max(0, 31 - total)
    pad = " ".join(["00"] * pad_len) if pad_len else ""

    adv_data = (
        f"sudo hcitool -i hci0 cmd 0x08 0x0008 "
        f"{total:02X} "
        f"02 01 06 "
        f"{rec_len:02X} FF {payload_hex} "
        f"{pad}"
    )
    run(adv_data)
    run("sudo hciconfig hci0 leadv 3")
    print(f"  -> 送信中: {label} (Manufacturer payload: {ascii_id}, デバイス名なし)")


def start_advertise_service_uuid(name, uuid16):
    stop_advertise()
    time.sleep(0.5)

    run("sudo hciconfig hci0 up")
    run('sudo hciconfig hci0 name "BLE Device"')

    low = uuid16 & 0xFF
    high = (uuid16 >> 8) & 0xFF
    adv_data = (
        f"sudo hcitool -i hci0 cmd 0x08 0x0008 "
        f"07 "
        f"02 01 06 "
        f"03 03 {low:02X} {high:02X} "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00"
    )
    run(adv_data)
    run("sudo hciconfig hci0 leadv 3")
    print(f"  -> 送信中: {name} (Service UUID: 0x{uuid16:04X}, デバイス名なし)")


def start_advertise_service_uuid128(name, uuid):
    stop_advertise()
    time.sleep(0.5)

    run("sudo hciconfig hci0 up")
    run('sudo hciconfig hci0 name "BLE Device"')

    raw = bytes.fromhex(uuid.replace("-", ""))
    le_hex = " ".join(f"{b:02X}" for b in raw[::-1])
    total = 3 + 18
    pad_len = max(0, 31 - total)
    pad = " ".join(["00"] * pad_len) if pad_len else ""
    adv_data = (
        f"sudo hcitool -i hci0 cmd 0x08 0x0008 "
        f"{total:02X} "
        f"02 01 06 "
        f"11 07 {le_hex} "
        f"{pad}"
    )
    run(adv_data)
    run("sudo hciconfig hci0 leadv 3")
    print(f"  -> 送信中: {name} (Service UUID: {uuid}, デバイス名なし)")


def start_advertise_appearance(name, appearance):
    stop_advertise()
    time.sleep(0.5)

    run("sudo hciconfig hci0 up")
    run('sudo hciconfig hci0 name "BLE Device"')

    low = appearance & 0xFF
    high = (appearance >> 8) & 0xFF
    adv_data = (
        f"sudo hcitool -i hci0 cmd 0x08 0x0008 "
        f"07 "
        f"02 01 06 "
        f"03 19 {low:02X} {high:02X} "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00 00 00 00 00 "
        f"00 00 00 00"
    )
    run(adv_data)
    run("sudo hciconfig hci0 leadv 3")
    print(f"  -> 送信中: {name} (Appearance: 0x{appearance:04X}, デバイス名なし)")


def print_menu():
    print("\n" + "=" * 55)
    print("  BLE Smart Glasses Emulator")
    print("  (スマートグラス検出アプリ テスト用)")
    print("=" * 55)
    print("\n--- Company ID 検出テスト ---")
    for num, (name, cid) in COMPANY_ID_DEVICES.items():
        print(f"  {num:2d}) {name} (0x{cid:04X})")
    print("\n--- デバイス名パターン検出テスト ---")
    for num, name in NAME_PATTERN_DEVICES.items():
        print(f"  {num:2d}) {name}")
    print("\n--- Service UUID 検出テスト ---")
    for num, (name, uuid16) in UUID_DEVICES.items():
        print(f"  {num:2d}) {name}")
    print("\n--- 128-bit Service UUID 検出テスト ---")
    for num, (name, uuid) in UUID128_DEVICES.items():
        print(f"  {num:2d}) {name}")
    print("\n--- 広告ペイロード検出テスト ---")
    for num, (name, payload) in PAYLOAD_DEVICES.items():
        print(f"  {num:2d}) {name} ({payload})")
    print("\n--- GAP Appearance 検出テスト ---")
    for num, (name, appearance) in APPEARANCE_DEVICES.items():
        print(f"  {num:2d}) {name} (0x{appearance:04X})")
    print("\n--- コントロール ---")
    print("  88) 全メーカー順番にテスト (各20秒)")
    print("  99) アドバタイズ停止")
    print("   0) 終了")
    print()


def auto_test_all():
    duration = 20
    print(f"\n全メーカーを {duration} 秒ずつテストします...")

    for num, (name, cid) in COMPANY_ID_DEVICES.items():
        print(f"\n[Company ID {num}] {name}")
        start_advertise_company_id(name, cid, name.split("(")[0].strip())
        time.sleep(duration)

    for num, name in NAME_PATTERN_DEVICES.items():
        print(f"\n[Name {num}] {name}")
        start_advertise_name_only(name)
        time.sleep(duration)

    for num, (name, uuid16) in UUID_DEVICES.items():
        print(f"\n[Service UUID {num}] {name}")
        start_advertise_service_uuid(name, uuid16)
        time.sleep(duration)

    for num, (name, uuid) in UUID128_DEVICES.items():
        print(f"\n[Service UUID128 {num}] {name}")
        start_advertise_service_uuid128(name, uuid)
        time.sleep(duration)

    for num, (name, payload) in PAYLOAD_DEVICES.items():
        print(f"\n[Payload {num}] {name}")
        start_advertise_ascii_manufacturer_payload(name, payload)
        time.sleep(duration)

    for num, (name, appearance) in APPEARANCE_DEVICES.items():
        print(f"\n[Appearance {num}] {name}")
        start_advertise_appearance(name, appearance)
        time.sleep(duration)

    stop_advertise()
    print("\n全テスト完了！")


def main():
    # Bluetooth が起動しているか確認
    result = subprocess.run(
        "hciconfig hci0", shell=True, capture_output=True, text=True
    )
    if "UP" not in result.stdout:
        print("Bluetooth を起動します...")
        run("sudo hciconfig hci0 up")
        time.sleep(1)

    print_menu()

    while True:
        try:
            choice = input("番号を選択 > ").strip()
            if not choice:
                continue
            num = int(choice)
        except ValueError:
            print("数字で入力してください")
            continue
        except (KeyboardInterrupt, EOFError):
            stop_advertise()
            print("\n終了します")
            sys.exit(0)

        if num == 0:
            stop_advertise()
            print("終了します")
            break
        elif num == 99:
            stop_advertise()
        elif num == 88:
            auto_test_all()
            print_menu()
        elif num in COMPANY_ID_DEVICES:
            name, cid = COMPANY_ID_DEVICES[num]
            start_advertise_company_id(name, cid, name.split("(")[0].strip())
        elif num in UUID_DEVICES:
            name, uuid16 = UUID_DEVICES[num]
            start_advertise_service_uuid(name, uuid16)
        elif num in UUID128_DEVICES:
            name, uuid = UUID128_DEVICES[num]
            start_advertise_service_uuid128(name, uuid)
        elif num in PAYLOAD_DEVICES:
            name, payload = PAYLOAD_DEVICES[num]
            start_advertise_ascii_manufacturer_payload(name, payload)
        elif num in APPEARANCE_DEVICES:
            name, appearance = APPEARANCE_DEVICES[num]
            start_advertise_appearance(name, appearance)
        elif num in NAME_PATTERN_DEVICES:
            dev_name = NAME_PATTERN_DEVICES[num]
            start_advertise_name_only(dev_name)
        else:
            print("無効な番号です")


if __name__ == "__main__":
    if subprocess.run("id -u", shell=True, capture_output=True, text=True).stdout.strip() != "0":
        print("sudo で実行してください: sudo python3 ble_smartglasses_emulator.py")
        sys.exit(1)
    main()
