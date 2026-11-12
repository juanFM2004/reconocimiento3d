"""
Enumera cámaras via comtypes DirectShow Device Enumeration (método correcto en Windows).
"""
import comtypes
import comtypes.client as cc

# GUIDs de DirectShow
CLSID_SystemDeviceEnum = comtypes.GUID("{62BE5D10-60EB-11d0-BD3B-00A0C911CE86}")
CLSID_VideoInputDeviceCategory = comtypes.GUID("{860BB310-5D01-11d0-BD3B-00A0C911CE86}")

try:
    from comtypes.gen import devenum_lib
except:
    pass

# Usar ICreateDevEnum interface  
IID_ICreateDevEnum = comtypes.GUID("{29840822-5B84-11D0-BD3B-00A0C911CE86}")
IID_IEnumMoniker   = comtypes.GUID("{00000102-0000-0000-C000-000000000046}")
IID_IMoniker       = comtypes.GUID("{0000000F-0000-0000-C000-000000000046}")
IID_IPropertyBag   = comtypes.GUID("{55272A00-42CB-11CE-8135-00AA004BB851}")

import ctypes
import ctypes.wintypes

# Cargar interfaces mediante ctypes COM directo
ole32  = ctypes.windll.ole32
oleaut = ctypes.windll.oleaut32

# CoCreateInstance para SystemDeviceEnum
class GUID(ctypes.Structure):
    _fields_ = [
        ("Data1", ctypes.c_ulong),
        ("Data2", ctypes.c_ushort),
        ("Data3", ctypes.c_ushort),
        ("Data4", ctypes.c_ubyte * 8)
    ]

# Usar winreg para listar cámaras registradas en CLSID de DSHOW Video Capture
import winreg

print("=== Cámaras registradas en DirectShow (CLSID_VideoInputDeviceCategory) ===")

VIDEO_CAT = r"SYSTEM\CurrentControlSet\Control\MediaSets\{083863F1-70DE-11D0-BD40-00A0C911CE86}\Instances"
# Alternativa más común:
VIDEO_CAT2 = r"SOFTWARE\Classes\CLSID\{860BB310-5D01-11d0-BD3B-00A0C911CE86}\Instance"

cameras_found = []
for reg_path in [VIDEO_CAT, VIDEO_CAT2]:
    try:
        hkey = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, reg_path)
        idx = 0
        while True:
            try:
                subkey_name = winreg.EnumKey(hkey, idx)
                subkey = winreg.OpenKey(hkey, subkey_name)
                try:
                    friendly_name = winreg.QueryValueEx(subkey, "FriendlyName")[0]
                    cameras_found.append(friendly_name)
                    print(f"  [{idx}] {friendly_name}  (key: {subkey_name})")
                except:
                    print(f"  [{idx}] (sin nombre amigable)  (key: {subkey_name})")
                winreg.CloseKey(subkey)
                idx += 1
            except OSError:
                break
        winreg.CloseKey(hkey)
        print(f"  (Desde: HKLM\\{reg_path})")
    except Exception as e:
        print(f"  (No se pudo leer {reg_path}: {e})")

if not cameras_found:
    # Buscar en HKCU también
    for reg_path in [VIDEO_CAT2]:
        try:
            hkey = winreg.OpenKey(winreg.HKEY_CURRENT_USER, reg_path)
            idx = 0
            while True:
                try:
                    subkey_name = winreg.EnumKey(hkey, idx)
                    subkey = winreg.OpenKey(hkey, subkey_name)
                    try:
                        friendly_name = winreg.QueryValueEx(subkey, "FriendlyName")[0]
                        cameras_found.append(friendly_name)
                        print(f"  [{idx}] {friendly_name} (HKCU)")
                    except:
                        pass
                    winreg.CloseKey(subkey)
                    idx += 1
                except OSError:
                    break
            winreg.CloseKey(hkey)
        except Exception as e:
            print(f"  (HKCU\\{reg_path}: {e})")

print(f"\nTotal cámaras DirectShow encontradas: {len(cameras_found)}")

# Ahora probar con OpenCV y el nombre exacto
if cameras_found:
    import cv2, numpy as np
    print("\n=== Probando con OpenCV CAP_DSHOW por nombre exacto ===")
    for name in cameras_found:
        path = f"video={name}"
        cap = cv2.VideoCapture(path, cv2.CAP_DSHOW)
        opened = cap.isOpened()
        brillo = -1
        if opened:
            ret, f = cap.read()
            if ret and f is not None:
                brillo = float(np.mean(f))
        cap.release()
        print(f"  '{name}' -> abierta={opened}, brillo={brillo:.1f}")
