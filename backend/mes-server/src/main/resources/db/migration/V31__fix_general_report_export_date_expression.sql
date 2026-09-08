-- UReport3 has no now() expression function. Replace it in the persisted
-- general template so preview and Excel export can render it successfully.
UPDATE ureport_file_tbl
SET content_ = REPLACE(
        content_,
        '<expression-value><![CDATA[now()]]></expression-value>',
        '<expression-value><![CDATA[date(''yyyy-MM-dd'')]]></expression-value>'
    ),
    update_time_ = NOW()
WHERE name_ = 'MES通用表格模板.ureport.xml'
  AND content_ LIKE '%<expression-value><![CDATA[now()]]></expression-value>%';
