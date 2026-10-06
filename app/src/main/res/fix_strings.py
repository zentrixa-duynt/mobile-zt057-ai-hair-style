import os
import re

base_path = '/Users/huuxuan/AndroidStudioProjects/AIHair/app/src/main/res/'

for dir_name in os.listdir(base_path):
    if dir_name.startswith('values-'):
        file_path = os.path.join(base_path, dir_name, 'strings.xml')
        if os.path.exists(file_path):
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Use regex to strip anything after (and including) the colon/space before %1$s
            # Examples:
            # "Reported: %1$s" -> "Reported"
            # "已举报：%1$s" -> "已举报"
            # "تم الإبلاغ عن: %1$s" -> "تم الإبلاغ عن"
            new_content = re.sub(
                r'<string name="msg_reported_reason">(.*?)(?:\s*[:：]\s*%1\$s|%1\$s)</string>',
                r'<string name="msg_reported_reason">\1</string>',
                content
            )
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Fixed {dir_name}")
