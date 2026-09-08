SET NAMES utf8mb4;

UPDATE pro_process_content SET content_text='安装电机到主机壳体，确认定位销与螺丝孔对齐', device='电批', material='M4螺丝', doc_url='/process/step-motor-01.jpg', remark='电机装配步骤1' WHERE process_id=1 AND order_num=1;
UPDATE pro_process_content SET content_text='锁附固定螺丝并校验扭矩值在标准范围内', device='扭矩扳手', material='M4螺丝', doc_url='/process/cover-motor.jpg', remark='电机装配步骤2' WHERE process_id=1 AND order_num=2;
UPDATE pro_process_content SET content_text='轻载通电测试电机转向与异响', device='测试仪', material='—', doc_url='/process/step-aging-01.jpg', remark='电机装配步骤3' WHERE process_id=1 AND order_num=3;

UPDATE pro_process_content SET content_text='扇叶按标记方向压装到电机轴并锁紧', device='压装治具', material='锁紧螺母', doc_url='/process/step-blade-01.jpg', remark='扇叶安装步骤1' WHERE process_id=2 AND order_num=1;
UPDATE pro_process_content SET content_text='动平衡检测并记录偏差值', device='动平衡仪', material='记录表', doc_url='/process/cover-blade.jpg', remark='扇叶安装步骤2' WHERE process_id=2 AND order_num=2;

UPDATE pro_process_content SET content_text='老化架通电运行，记录电流与噪音', device='老化架', material='记录表', doc_url='/process/step-aging-01.jpg', remark='老化测试步骤1' WHERE process_id=3 AND order_num=1;
UPDATE pro_process_content SET content_text='连续运行30分钟后复测并贴合格标签', device='老化架', material='合格标签', doc_url='/process/cover-aging.jpg', remark='老化测试步骤2' WHERE process_id=3 AND order_num=2;

UPDATE pro_process_content SET content_text='放入缓冲泡沫后封箱并贴标签', device='封箱机', material='纸箱', doc_url='/process/step-pack-01.jpg', remark='包装步骤1' WHERE process_id=4 AND order_num=1;
UPDATE pro_process_content SET content_text='扫码核对型号并打印外箱标签', device='条码打印机', material='外箱标签', doc_url='/process/cover-pack.jpg', remark='包装步骤2' WHERE process_id=4 AND order_num=2;
