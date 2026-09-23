import os
import xml.etree.ElementTree as ET

strings = {
    'values': {
        'text_no_internet_connection': 'No Internet Connection',
        'text_please_turn_on_wifi': 'Please turn on and connect to a Wi-Fi network',
        'text_go_to_setting': 'Go to setting'
    },
    'values-af': {
        'text_no_internet_connection': 'Geen internetverbinding nie',
        'text_please_turn_on_wifi': 'Skakel asseblief aan en koppel aan \'n Wi-Fi-netwerk',
        'text_go_to_setting': 'Gaan na instelling'
    },
    'values-ar': {
        'text_no_internet_connection': 'لا يوجد اتصال بالإنترنت',
        'text_please_turn_on_wifi': 'يرجى التشغيل والاتصال بشبكة Wi-Fi',
        'text_go_to_setting': 'اذهب إلى الإعدادات'
    },
    'values-bn': {
        'text_no_internet_connection': 'কোনো ইন্টারনেট সংযোগ নেই',
        'text_please_turn_on_wifi': 'অনুগ্রহ করে চালু করুন এবং একটি ওয়াই-ফাই নেটওয়ার্কের সাথে সংযোগ করুন',
        'text_go_to_setting': 'সেটিং এ যান'
    },
    'values-de': {
        'text_no_internet_connection': 'Keine Internetverbindung',
        'text_please_turn_on_wifi': 'Bitte aktivieren Sie und stellen Sie eine Verbindung zu einem WLAN-Netzwerk her',
        'text_go_to_setting': 'Gehe zu den Einstellungen'
    },
    'values-es': {
        'text_no_internet_connection': 'Sin conexión a Internet',
        'text_please_turn_on_wifi': 'Por favor, activa y conéctate a una red Wi-Fi',
        'text_go_to_setting': 'Ir a configuración'
    },
    'values-fr': {
        'text_no_internet_connection': 'Pas de connexion Internet',
        'text_please_turn_on_wifi': 'Veuillez activer et vous connecter à un réseau Wi-Fi',
        'text_go_to_setting': 'Aller aux paramètres'
    },
    'values-hi': {
        'text_no_internet_connection': 'कोई इंटरनेट कनेक्शन नहीं',
        'text_please_turn_on_wifi': 'कृपया चालू करें और एक वाई-फाई नेटवर्क से कनेक्ट करें',
        'text_go_to_setting': 'सेटिंग पर जाएं'
    },
    'values-hr': {
        'text_no_internet_connection': 'Nema internetske veze',
        'text_please_turn_on_wifi': 'Uključite i povežite se s Wi-Fi mrežom',
        'text_go_to_setting': 'Idi na postavke'
    },
    'values-hu': {
        'text_no_internet_connection': 'Nincs internetkapcsolat',
        'text_please_turn_on_wifi': 'Kérjük, kapcsolja be és csatlakozzon egy Wi-Fi hálózathoz',
        'text_go_to_setting': 'Ugrás a beállításokhoz'
    },
    'values-in': {
        'text_no_internet_connection': 'Tidak Ada Koneksi Internet',
        'text_please_turn_on_wifi': 'Silakan nyalakan dan sambungkan ke jaringan Wi-Fi',
        'text_go_to_setting': 'Pergi ke pengaturan'
    },
    'values-it': {
        'text_no_internet_connection': 'Nessuna connessione Internet',
        'text_please_turn_on_wifi': 'Si prega di attivare e connettersi a una rete Wi-Fi',
        'text_go_to_setting': 'Vai alle impostazioni'
    },
    'values-ja': {
        'text_no_internet_connection': 'インターネット接続がありません',
        'text_please_turn_on_wifi': 'Wi-Fiをオンにしてネットワークに接続してください',
        'text_go_to_setting': '設定へ行く'
    },
    'values-ko': {
        'text_no_internet_connection': '인터넷 연결 없음',
        'text_please_turn_on_wifi': 'Wi-Fi를 켜고 네트워크에 연결해 주세요',
        'text_go_to_setting': '설정으로 이동'
    },
    'values-ne': {
        'text_no_internet_connection': 'कुनै इन्टरनेट जडान छैन',
        'text_please_turn_on_wifi': 'कृपया सक्रिय गर्नुहोस् र Wi-Fi नेटवर्कमा जडान गर्नुहोस्',
        'text_go_to_setting': 'सेटिङमा जानुहोस्'
    },
    'values-nl': {
        'text_no_internet_connection': 'Geen internetverbinding',
        'text_please_turn_on_wifi': 'Schakel in en maak verbinding met een wifi-netwerk',
        'text_go_to_setting': 'Ga naar instellingen'
    },
    'values-pt': {
        'text_no_internet_connection': 'Sem conexão com a Internet',
        'text_please_turn_on_wifi': 'Por favor, ligue e conecte-se a uma rede Wi-Fi',
        'text_go_to_setting': 'Ir para as configurações'
    },
    'values-ru': {
        'text_no_internet_connection': 'Нет подключения к Интернету',
        'text_please_turn_on_wifi': 'Пожалуйста, включите и подключитесь к сети Wi-Fi',
        'text_go_to_setting': 'Перейти в настройки'
    },
    'values-th': {
        'text_no_internet_connection': 'ไม่มีการเชื่อมต่ออินเทอร์เน็ต',
        'text_please_turn_on_wifi': 'โปรดเปิดและเชื่อมต่อกับเครือข่าย Wi-Fi',
        'text_go_to_setting': 'ไปที่การตั้งค่า'
    },
    'values-tl': {
        'text_no_internet_connection': 'Walang Koneksyon sa Internet',
        'text_please_turn_on_wifi': 'Mangyaring i-on at kumonekta sa isang Wi-Fi network',
        'text_go_to_setting': 'Pumunta sa setting'
    },
    'values-tr': {
        'text_no_internet_connection': 'İnternet Bağlantısı Yok',
        'text_please_turn_on_wifi': 'Lütfen açın ve bir Wi-Fi ağına bağlanın',
        'text_go_to_setting': 'Ayarlara git'
    },
    'values-uk': {
        'text_no_internet_connection': 'Немає підключення до Інтернету',
        'text_please_turn_on_wifi': 'Будь ласка, увімкніть і підключіться до мережі Wi-Fi',
        'text_go_to_setting': 'Перейти до налаштувань'
    },
    'values-ur': {
        'text_no_internet_connection': 'انٹرنیٹ کنکشن نہیں ہے',
        'text_please_turn_on_wifi': 'براہ کرم آن کریں اور Wi-Fi نیٹ ورک سے جڑیں',
        'text_go_to_setting': 'ترتیبات میں جائیں'
    },
    'values-vi': {
        'text_no_internet_connection': 'Không có kết nối Internet',
        'text_please_turn_on_wifi': 'Vui lòng bật và kết nối với mạng Wi-Fi',
        'text_go_to_setting': 'Đi tới cài đặt'
    },
    'values-zh': {
        'text_no_internet_connection': '没有网络连接',
        'text_please_turn_on_wifi': '请开启并连接到Wi-Fi网络',
        'text_go_to_setting': '前往设置'
    }
}

res_path = 'app/src/main/res'
for folder, string_dict in strings.items():
    folder_path = os.path.join(res_path, folder)
    if os.path.exists(folder_path):
        strings_xml = os.path.join(folder_path, 'strings.xml')
        if os.path.exists(strings_xml):
            with open(strings_xml, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Simple string manipulation to append before </resources>
            # Avoid duplicate inserts
            if 'name="text_no_internet_connection"' not in content:
                insert_pos = content.rfind('</resources>')
                if insert_pos != -1:
                    new_strings = "\n"
                    for k, v in string_dict.items():
                        # Escape single quotes and ampersands
                        v = v.replace("&", "&amp;").replace("'", "\\'")
                        new_strings += f'    <string name="{k}">{v}</string>\n'
                    new_content = content[:insert_pos] + new_strings + content[insert_pos:]
                    with open(strings_xml, 'w', encoding='utf-8') as f:
                        f.write(new_content)

print("Translations added successfully.")
