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

    # Flags + Complete Local Name
    name_bytes = device_name.encode("ascii")
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
    print("\n--- コントロール ---")
    print("  88) 全メーカー順番にテスト (各20秒)")
    print("  99) アドバタイズ停止")
    print("   0) 終了")
    print()


def auto_test_all():
    duration = 20
    print(f"\n全メーカーを {duration} 秒ずつテストします...")

    for num, (name, cid) in COMPANY_ID_DEVICES.items():
        print(f"\n[{num}/{len(COMPANY_ID_DEVICES) + len(NAME_PATTERN_DEVICES)}] {name}")
        start_advertise_company_id(name, cid, name.split("(")[0].strip())
        time.sleep(duration)

    for num, dev_name in NAME_PATTERN_DEVICES.items():
        print(f"\n[Name Pattern] {dev_name}")
        start_advertise_name_only(dev_name)
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
