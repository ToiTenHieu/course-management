-- Update only the exact product-owned sample sentence; preserve all other lesson content.
UPDATE lessons
SET text_content = REPLACE(text_content,
    'Đây là nội dung mẫu của học viện để trình diễn luồng học tập.',
    'Đây là nội dung mẫu của Course Management để trình diễn luồng học tập.')
WHERE text_content LIKE '%Đây là nội dung mẫu của học viện để trình diễn luồng học tập.%';
