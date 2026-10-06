import os
import re

base_path = '/Users/huuxuan/AndroidStudioProjects/AIHair/app/src/main/res/'

translations = {
    'af': {
        'text_flag_offensive_content': 'Rapporteer aanstootlike inhoud',
        'text_spam': 'Gemorspos',
        'text_inappropriate_content': 'Onvanpaste inhoud',
        'text_copyright': 'Kopiereg',
        'text_offensive': 'Aanstootlik',
        'text_cancel': 'Kanselleer',
        'text_report': 'Rapporteer',
        'msg_please_select_reason': "Kies asseblief 'n rede",
        'msg_reported_reason': 'Gerapporteer: %1$s'
    },
    'ar': {
        'text_flag_offensive_content': 'الإبلاغ عن محتوى مسيء',
        'text_spam': 'بريد مزعج',
        'text_inappropriate_content': 'محتوى غير لائق',
        'text_copyright': 'حقوق النشر',
        'text_offensive': 'مسيء',
        'text_cancel': 'إلغاء',
        'text_report': 'إبلاغ',
        'msg_please_select_reason': 'يرجى تحديد سبب',
        'msg_reported_reason': 'تم الإبلاغ عن: %1$s'
    },
    'bn': {
        'text_flag_offensive_content': 'আপত্তিকর বিষয়বস্তু ফ্ল্যাগ করুন',
        'text_spam': 'স্প্যাম',
        'text_inappropriate_content': 'অনুপযুক্ত বিষয়বস্তু',
        'text_copyright': 'কপিরাইট',
        'text_offensive': 'আপত্তিকর',
        'text_cancel': 'বাতিল করুন',
        'text_report': 'রিপোর্ট করুন',
        'msg_please_select_reason': 'অনুগ্রহ করে একটি কারণ নির্বাচন করুন',
        'msg_reported_reason': 'রিপোর্ট করা হয়েছে: %1$s'
    },
    'de': {
        'text_flag_offensive_content': 'Anstößige Inhalte melden',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Unangemessene Inhalte',
        'text_copyright': 'Urheberrecht',
        'text_offensive': 'Anstößig',
        'text_cancel': 'Abbrechen',
        'text_report': 'Melden',
        'msg_please_select_reason': 'Bitte wählen Sie einen Grund',
        'msg_reported_reason': 'Gemeldet: %1$s'
    },
    'es': {
        'text_flag_offensive_content': 'Marcar contenido ofensivo',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Contenido inapropiado',
        'text_copyright': 'Derechos de autor',
        'text_offensive': 'Ofensivo',
        'text_cancel': 'Cancelar',
        'text_report': 'Denunciar',
        'msg_please_select_reason': 'Seleccione un motivo',
        'msg_reported_reason': 'Denunciado: %1$s'
    },
    'fr': {
        'text_flag_offensive_content': 'Signaler un contenu offensant',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Contenu inapproprié',
        'text_copyright': "Droit d\\'auteur",
        'text_offensive': 'Offensant',
        'text_cancel': 'Annuler',
        'text_report': 'Signaler',
        'msg_please_select_reason': 'Veuillez sélectionner un motif',
        'msg_reported_reason': 'Signalé: %1$s'
    },
    'hi': {
        'text_flag_offensive_content': 'आपत्तिजनक सामग्री फ़्लैग करें',
        'text_spam': 'स्पैम',
        'text_inappropriate_content': 'अनुचित सामग्री',
        'text_copyright': 'कॉपीराइट',
        'text_offensive': 'आपत्तिजनक',
        'text_cancel': 'रद्द करें',
        'text_report': 'रिपोर्ट करें',
        'msg_please_select_reason': 'कृपया एक कारण चुनें',
        'msg_reported_reason': 'रिपोर्ट किया गया: %1$s'
    },
    'hr': {
        'text_flag_offensive_content': 'Označi uvredljiv sadržaj',
        'text_spam': 'Neželjeni sadržaj',
        'text_inappropriate_content': 'Neprikladan sadržaj',
        'text_copyright': 'Autorska prava',
        'text_offensive': 'Uvredljivo',
        'text_cancel': 'Odustani',
        'text_report': 'Prijavi',
        'msg_please_select_reason': 'Odaberite razlog',
        'msg_reported_reason': 'Prijavljeno: %1$s'
    },
    'hu': {
        'text_flag_offensive_content': 'Sértő tartalom megjelölése',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Nem megfelelő tartalom',
        'text_copyright': 'Szerzői jog',
        'text_offensive': 'Sértő',
        'text_cancel': 'Mégse',
        'text_report': 'Jelentés',
        'msg_please_select_reason': 'Kérjük, válasszon egy okot',
        'msg_reported_reason': 'Jelentve: %1$s'
    },
    'in': {
        'text_flag_offensive_content': 'Tandai konten yang menyinggung',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Konten tidak pantas',
        'text_copyright': 'Hak cipta',
        'text_offensive': 'Menyinggung',
        'text_cancel': 'Batal',
        'text_report': 'Laporkan',
        'msg_please_select_reason': 'Silakan pilih alasan',
        'msg_reported_reason': 'Dilaporkan: %1$s'
    },
    'it': {
        'text_flag_offensive_content': 'Segnala contenuto offensivo',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Contenuto inappropriato',
        'text_copyright': 'Copyright',
        'text_offensive': 'Offensivo',
        'text_cancel': 'Annulla',
        'text_report': 'Segnala',
        'msg_please_select_reason': 'Seleziona un motivo',
        'msg_reported_reason': 'Segnalato: %1$s'
    },
    'ja': {
        'text_flag_offensive_content': '不適切なコンテンツを報告',
        'text_spam': 'スパム',
        'text_inappropriate_content': '不適切なコンテンツ',
        'text_copyright': '著作権',
        'text_offensive': '不快',
        'text_cancel': 'キャンセル',
        'text_report': '報告する',
        'msg_please_select_reason': '理由を選択してください',
        'msg_reported_reason': '報告済み: %1$s'
    },
    'ko': {
        'text_flag_offensive_content': '불쾌한 콘텐츠 신고',
        'text_spam': '스팸',
        'text_inappropriate_content': '부적절한 콘텐츠',
        'text_copyright': '저작권',
        'text_offensive': '불쾌함',
        'text_cancel': '취소',
        'text_report': '신고',
        'msg_please_select_reason': '사유를 선택하세요',
        'msg_reported_reason': '신고됨: %1$s'
    },
    'ne': {
        'text_flag_offensive_content': 'आपत्तिजनक सामग्री फ्ल्याग गर्नुहोस्',
        'text_spam': 'स्पाम',
        'text_inappropriate_content': 'अनुपयुक्त सामग्री',
        'text_copyright': 'प्रतिलिपि अधिकार',
        'text_offensive': 'आपत्तिजनक',
        'text_cancel': 'रद्द गर्नुहोस्',
        'text_report': 'रिपोर्ट गर्नुहोस्',
        'msg_please_select_reason': 'कृपया कारण चयन गर्नुहोस्',
        'msg_reported_reason': 'रिपोर्ट गरियो: %1$s'
    },
    'nl': {
        'text_flag_offensive_content': 'Aanstootgevende inhoud markeren',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Ongepaste inhoud',
        'text_copyright': 'Auteursrecht',
        'text_offensive': 'Aanstootgevend',
        'text_cancel': 'Annuleren',
        'text_report': 'Melden',
        'msg_please_select_reason': 'Selecteer een reden',
        'msg_reported_reason': 'Gemeld: %1$s'
    },
    'pt': {
        'text_flag_offensive_content': 'Sinalizar conteúdo ofensivo',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Conteúdo inapropriado',
        'text_copyright': 'Direitos autorais',
        'text_offensive': 'Ofensivo',
        'text_cancel': 'Cancelar',
        'text_report': 'Denunciar',
        'msg_please_select_reason': 'Selecione um motivo',
        'msg_reported_reason': 'Denunciado: %1$s'
    },
    'ru': {
        'text_flag_offensive_content': 'Отметить оскорбительный контент',
        'text_spam': 'Спам',
        'text_inappropriate_content': 'Неприемлемый контент',
        'text_copyright': 'Авторские права',
        'text_offensive': 'Оскорбительно',
        'text_cancel': 'Отмена',
        'text_report': 'Пожаловаться',
        'msg_please_select_reason': 'Выберите причину',
        'msg_reported_reason': 'Жалоба отправлена: %1$s'
    },
    'th': {
        'text_flag_offensive_content': 'รายงานเนื้อหาไม่เหมาะสม',
        'text_spam': 'สแปม',
        'text_inappropriate_content': 'เนื้อหาไม่เหมาะสม',
        'text_copyright': 'ลิขสิทธิ์',
        'text_offensive': 'ล่วงละเมิด',
        'text_cancel': 'ยกเลิก',
        'text_report': 'รายงาน',
        'msg_please_select_reason': 'โปรดเลือกเหตุผล',
        'msg_reported_reason': 'รายงานแล้ว: %1$s'
    },
    'tl': {
        'text_flag_offensive_content': 'I-flag ang nakakasakit na nilalaman',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Hindi naaangkop na nilalaman',
        'text_copyright': 'Copyright',
        'text_offensive': 'Nakakasakit',
        'text_cancel': 'Kanselahin',
        'text_report': 'I-report',
        'msg_please_select_reason': 'Mangyaring pumili ng dahilan',
        'msg_reported_reason': 'Naiulat: %1$s'
    },
    'tr': {
        'text_flag_offensive_content': 'Rahatsız edici içeriği işaretle',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Uygunsuz içerik',
        'text_copyright': 'Telif hakkı',
        'text_offensive': 'Rahatsız edici',
        'text_cancel': 'İptal',
        'text_report': 'Bildir',
        'msg_please_select_reason': 'Lütfen bir neden seçin',
        'msg_reported_reason': 'Bildirildi: %1$s'
    },
    'uk': {
        'text_flag_offensive_content': 'Поскаржитися на неприйнятний контент',
        'text_spam': 'Спам',
        'text_inappropriate_content': 'Неприйнятний контент',
        'text_copyright': 'Авторські права',
        'text_offensive': 'Образливо',
        'text_cancel': 'Скасувати',
        'text_report': 'Поскаржитися',
        'msg_please_select_reason': 'Будь ласка, виберіть причину',
        'msg_reported_reason': 'Поскаржилися: %1$s'
    },
    'ur': {
        'text_flag_offensive_content': 'نامناسب مواد کی اطلاع دیں',
        'text_spam': 'سپیم',
        'text_inappropriate_content': 'نامناسب مواد',
        'text_copyright': 'کاپی رائٹ',
        'text_offensive': 'جارحانہ',
        'text_cancel': 'منسوخ کریں',
        'text_report': 'رپورٹ کریں',
        'msg_please_select_reason': 'براہ کرم ایک وجہ منتخب کریں',
        'msg_reported_reason': 'رپورٹ کیا گیا: %1$s'
    },
    'vi': {
        'text_flag_offensive_content': 'Báo cáo nội dung vi phạm',
        'text_spam': 'Spam',
        'text_inappropriate_content': 'Nội dung không phù hợp',
        'text_copyright': 'Bản quyền',
        'text_offensive': 'Nội dung phản cảm',
        'text_cancel': 'Hủy',
        'text_report': 'Báo cáo',
        'msg_please_select_reason': 'Vui lòng chọn một lý do',
        'msg_reported_reason': 'Đã báo cáo: %1$s'
    },
    'zh': {
        'text_flag_offensive_content': '标记违规内容',
        'text_spam': '垃圾内容',
        'text_inappropriate_content': '不适当内容',
        'text_copyright': '版权',
        'text_offensive': '冒犯性',
        'text_cancel': '取消',
        'text_report': '举报',
        'msg_please_select_reason': '请选择一个原因',
        'msg_reported_reason': '已举报：%1$s'
    }
}

for lang, strings in translations.items():
    file_path = os.path.join(base_path, f'values-{lang}', 'strings.xml')
    if os.path.exists(file_path):
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if already added
        if 'text_flag_offensive_content' in content:
            print(f"Already translated for {lang}")
            continue

        insertion = '\n    <!-- Report Dialog -->\n'
        for key, value in strings.items():
            # escape quotes
            value = value.replace("'", "\\'")
            insertion += f'    <string name="{key}">{value}</string>\n'
        
        # Replace </resources>
        content = re.sub(r'</resources>', f'{insertion}</resources>', content)
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Translated {lang}")
    else:
        print(f"File not found for {lang}")
