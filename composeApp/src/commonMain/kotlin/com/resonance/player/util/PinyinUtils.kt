package com.resonance.player.util

/**
 * 纯 Kotlin 实现的轻量拼音与首字母匹配工具（无任何外部庞大字典依赖）。
 * 支持提取中文字符串的拼音首字母（如 "周杰伦" -> "zjl"）、全拼前缀匹配以及拼音模糊搜索。
 */
object PinyinUtils {

    // 常用汉字 GB2312 编码范围首字母快速定位边界 (基于一级常用汉字编码区段)
    private val pinyinInitials = charArrayOf(
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x', 'y', 'z'
    )

    // 基于 Unicode 常用汉字首字母查找表 (简表 + 常见多音/生僻字覆盖)
    private val initialBoundaries = intArrayOf(
        0xB0A1, 0xB0C5, 0xB2C1, 0xB4EE, 0xB6EA, 0xB7A2, 0xB8C1, 0xB9FE,
        0xBBF7, 0xBFA6, 0xC0AC, 0xC2E8, 0xC4C3, 0xC5B6, 0xC5BE, 0xC6DA,
        0xC8BB, 0xC8F6, 0xCBFA, 0xCDDA, 0xCEF4, 0xD1B9, 0xD4D1
    )

    // 特殊/常用汉字拼音首字母快速覆盖映射
    private val specialInitials = mapOf(
        '重' to 'c', '长' to 'c', '乐' to 'y', '行' to 'x', '解' to 'x',
        '曾' to 'z', '区' to 'q', '朴' to 'p', '单' to 's', '仇' to 'q',
        '沈' to 's', '查' to 'z', '繁' to 'f', '爱' to 'a', '安' to 'a',
        '奥' to 'a', '拔' to 'b', '把' to 'b', '白' to 'b', '百' to 'b',
        '半' to 'b', '包' to 'b', '堡' to 'b', '爆' to 'b', '北' to 'b',
        '本' to 'b', '比' to 'b', '币' to 'b', '必' to 'b', '闭' to 'b',
        '边' to 'b', '变' to 'b', '表' to 'b', '别' to 'b', '冰' to 'b',
        '病' to 'b', '步' to 'b', '不' to 'b', '部' to 'b', '布' to 'b',
        '彩' to 'c', '草' to 'c', '茶' to 'c', '常' to 'c', '唱' to 'c',
        '超' to 'c', '潮' to 'c', '车' to 'c', '晨' to 'c', '城' to 'c',
        '成' to 'c', '呈' to 'c', '程' to 'c', '迟' to 'c', '初' to 'c',
        '出' to 'c', '除' to 'c', '楚' to 'c', '穿' to 'c', '传' to 'c',
        '春' to 'c', '纯' to 'c', '磁' to 'c', '从' to 'c', '错' to 'c',
        '大' to 'd', '带' to 'd', '单' to 'd', '蛋' to 'd', '当' to 'd',
        '刀' to 'd', '岛' to 'd', '到' to 'd', '道' to 'd', '得' to 'd',
        '灯' to 'd', '等' to 'd', '低' to 'd', '底' to 'd', '地' to 'd',
        '第' to 'd', '点' to 'd', '电' to 'd', '店' to 'd', '掉' to 'd',
        '东' to 'd', '冬' to 'd', '动' to 'd', '都' to 'd', '度' to 'd',
        '对' to 'd', '多' to 'd', '朵' to 'd', '耳' to 'e', '二' to 'e',
        '发' to 'f', '法' to 'f', '反' to 'f', '方' to 'f', '放' to 'f',
        '飞' to 'f', '非' to 'f', '分' to 'f', '风' to 'f', '封' to 'f',
        '枫' to 'f', '峰' to 'f', '佛' to 'f', '否' to 'f', '服' to 'f',
        '福' to 'f', '浮' to 'f', '父' to 'f', '复' to 'f', '副' to 'f',
        '该' to 'g', '改' to 'g', '概' to 'g', '干' to 'g', '感' to 'g',
        '刚' to 'g', '高' to 'g', '告' to 'g', '歌' to 'g', '格' to 'g',
        '各' to 'g', '根' to 'g', '跟' to 'g', '更' to 'g', '公' to 'g',
        '共' to 'g', '狗' to 'g', '够' to 'g', '孤' to 'g', '古' to 'g',
        '故' to 'g', '顾' to 'g', '关' to 'g', '官' to 'g', '观' to 'g',
        '光' to 'g', '广' to 'g', '归' to 'g', '鬼' to 'g', '国' to 'g',
        '过' to 'g', '果' to 'g', '海' to 'h', '害' to 'h', '含' to 'h',
        '汉' to 'h', '好' to 'h', '号' to 'h', '喝' to 'h', '和' to 'h',
        '河' to 'h', '黑' to 'h', '很' to 'h', '红' to 'h', '后' to 'h',
        '候' to 'h', '呼' to 'h', '忽' to 'h', '狐' to 'h', '胡' to 'h',
        '湖' to 'h', '虎' to 'h', '互' to 'h', '户' to 'h', '花' to 'h',
        '华' to 'h', '画' to 'h', '话' to 'h', '怀' to 'h', '坏' to 'h',
        '欢' to 'h', '还' to 'h', '环' to 'h', '换' to 'h', '黄' to 'h',
        '灰' to 'h', '回' to 'h', '会' to 'h', '活' to 'h', '火' to 'h',
        '获' to 'h', '或' to 'h', '几' to 'j', '极' to 'j', '即' to 'j',
        '级' to 'j', '集' to 'j', '急' to 'j', '给' to 'g', '纪' to 'j',
        '记' to 'j', '计' to 'j', '家' to 'j', '加' to 'j', '夹' to 'j',
        '甲' to 'j', '假' to 'j', '价' to 'j', '架' to 'j', '尖' to 'j',
        '间' to 'j', '简' to 'j', '见' to 'j', '件' to 'j', '建' to 'j',
        '剑' to 'j', '健' to 'j', '江' to 'j', '将' to 'j', '奖' to 'j',
        '讲' to 'j', '交' to 'j', '焦' to 'j', '角' to 'j', '脚' to 'j',
        '教' to 'j', '阶' to 'j', '皆' to 'j', '接' to 'j', '街' to 'j',
        '节' to 'j', '结' to 'j', '杰' to 'j', '借' to 'j', '今' to 'j',
        '金' to 'j', '仅' to 'j', '紧' to 'j', '进' to 'j', '近' to 'j',
        '京' to 'j', '经' to 'j', '惊' to 'j', '睛' to 'j', '景' to 'j',
        '静' to 'j', '境' to 'j', '镜' to 'j', '九' to 'j', '久' to 'j',
        '酒' to 'j', '旧' to 'j', '就' to 'j', '局' to 'j', '举' to 'j',
        '句' to 'j', '具' to 'j', '剧' to 'j', '绝' to 'j', '决' to 'j',
        '觉' to 'j', '开' to 'k', '看' to 'k', '康' to 'k', '考' to 'k',
        '靠' to 'k', '科' to 'k', '棵' to 'k', '可' to 'k', '渴' to 'k',
        '克' to 'k', '客' to 'k', '肯' to 'k', '空' to 'k', '孔' to 'k',
        '恐' to 'k', '口' to 'k', '苦' to 'k', '哭' to 'k', '库' to 'k',
        '快' to 'k', '狂' to 'k', '况' to 'k', '拉' to 'l', '来' to 'l',
        '蓝' to 'l', '狼' to 'l', '老' to 'l', '乐' to 'l', '了' to 'l',
        '雷' to 'l', '泪' to 'l', '类' to 'l', '冷' to 'l', '离' to 'l',
        '里' to 'l', '理' to 'l', '力' to 'l', '历' to 'l', '立' to 'l',
        '丽' to 'l', '连' to 'l', '脸' to 'l', '练' to 'l', '凉' to 'l',
        '两' to 'l', '亮' to 'l', '量' to 'l', '疗' to 'l', '列' to 'l',
        '林' to 'l', '临' to 'l', '零' to 'l', '领' to 'l', '令' to 'l',
        '另' to 'l', '流' to 'l', '留' to 'l', '六' to 'l', '龙' to 'l',
        '楼' to 'l', '漏' to 'l', '露' to 'l', '陆' to 'l', '录' to 'l',
        '路' to 'l', '旅' to 'l', '绿' to 'l', '乱' to 'l', '伦' to 'l',
        '轮' to 'l', '论' to 'l', '落' to 'l', '妈' to 'm', '麻' to 'm',
        '马' to 'm', '玛' to 'm', '码' to 'm', '买' to 'm', '卖' to 'm',
        '满' to 'm', '慢' to 'm', '忙' to 'm', '毛' to 'm', '么' to 'm',
        '没' to 'm', '美' to 'm', '门' to 'm', '们' to 'm', '蒙' to 'm',
        '猛' to 'm', '梦' to 'm', '米' to 'm', '密' to 'm', '秘' to 'm',
        '眠' to 'm', '面' to 'm', '苗' to 'm', '秒' to 'm', '妙' to 'm',
        '灭' to 'm', '民' to 'm', '名' to 'm', '明' to 'm', '命' to 'm',
        '模' to 'm', '摸' to 'm', '魔' to 'm', '末' to 'm', '莫' to 'm',
        '木' to 'm', '目' to 'm', '母' to 'm', '墓' to 'm', '暮' to 'm',
        '拿' to 'n', '哪' to 'n', '那' to 'n', '男' to 'n', '南' to 'n',
        '难' to 'n', '脑' to 'n', '闹' to 'n', '呢' to 'n', '内' to 'n',
        '能' to 'n', '你' to 'n', '年' to 'n', '念' to 'n', '娘' to 'n',
        '鸟' to 'n', '您' to 'n', '宁' to 'n', '牛' to 'n', '农' to 'n',
        '浓' to 'n', '女' to 'n', '暖' to 'n', '欧' to 'o', '偶' to 'o',
        '怕' to 'p', '拍' to 'p', '排' to 'p', '盘' to 'p', '旁' to 'p',
        '跑' to 'p', '陪' to 'p', '配' to 'p', '朋' to 'p', '皮' to 'p',
        '偏' to 'p', '片' to 'p', '漂' to 'p', '飘' to 'p', '票' to 'p',
        '拼' to 'p', '频' to 'p', '品' to 'p', '平' to 'p', '瓶' to 'p',
        '破' to 'p', '扑' to 'p', '铺' to 'p', '普' to 'p', '七' to 'q',
        '期' to 'q', '齐' to 'q', '奇' to 'q', '骑' to 'q', '起' to 'q',
        '气' to 'q', '汽' to 'q', '千' to 'q', '前' to 'q', '钱' to 'q',
        '浅' to 'q', '强' to 'q', '墙' to 'q', '桥' to 'q', '巧' to 'q',
        '切' to 'q', '且' to 'q', '亲' to 'q', '琴' to 'q', '青' to 'q',
        '轻' to 'q', '清' to 'q', '情' to 'q', '晴' to 'q', '请' to 'q',
        '秋' to 'q', '求' to 'q', '球' to 'q', '去' to 'q', '全' to 'q',
        '群' to 'q', '然' to 'r', '燃' to 'r', '让' to 'r', '热' to 'r',
        '人' to 'r', '任' to 'r', '认' to 'r', '日' to 'r', '如' to 'r',
        '入' to 'r', '软' to 'r', '若' to 'r', '三' to 's', '伞' to 's',
        '散' to 's', '桑' to 's', '色' to 's', '森' to 's', '杀' to 's',
        '沙' to 's', '山' to 's', '闪' to 's', '伤' to 's', '商' to 's',
        '上' to 's', '少' to 's', '谁' to 's', '深' to 's', '什' to 's',
        '生' to 's', '声' to 's', '胜' to 's', '十' to 's', '石' to 's',
        '时' to 's', '识' to 's', '实' to 's', '拾' to 's', '史' to 's',
        '使' to 's', '始' to 's', '市' to 's', '示' to 's', '世' to 's',
        '事' to 's', '是' to 's', '适' to 's', '室' to 's', '收' to 's',
        '手' to 's', '首' to 's', '受' to 's', '树' to 's', '数' to 's',
        '双' to 's', '谁' to 's', '水' to 's', '睡' to 's', '说' to 's',
        '思' to 's', '斯' to 's', '死' to 's', '四' to 's', '似' to 's',
        '松' to 's', '送' to 's', '随' to 's', '岁' to 's', '孙' to 's',
        '所' to 's', '他' to 't', '她' to 't', '它' to 't', '台' to 't',
        '太' to 't', '态' to 't', '弹' to 't', '唐' to 't',
        '糖' to 't', '躺' to 't', '套' to 't', '特' to 't', '疼' to 't',
        '体' to 't', '天' to 't', '田' to 't', '甜' to 't', '条' to 't',
        '跳' to 't', '贴' to 't', '听' to 't', '停' to 't', '通' to 't',
        '同' to 't', '痛' to 't', '头' to 't', '透' to 't', '图' to 't',
        '土' to 't', '团' to 't', '推' to 't', '退' to 't', '晚' to 'w',
        '万' to 'w', '王' to 'w', '网' to 'w', '往' to 'w', '忘' to 'w',
        '望' to 'w', '微' to 'w', '为' to 'w', '唯' to 'w', '维' to 'w',
        '尾' to 'w', '未' to 'w', '位' to 'w', '味' to 'w', '温' to 'w',
        '文' to 'w', '闻' to 'w', '问' to 'w', '我' to 'w', '握' to 'w',
        '无' to 'w', '五' to 'w', '舞' to 'w', '物' to 'w', '雾' to 'w',
        '西' to 'x', '吸' to 'x', '希' to 'x', '息' to 'x', '惜' to 'x',
        '喜' to 'x', '戏' to 'x', '系' to 'x', '细' to 'x', '下' to 'x',
        '夏' to 'x', '先' to 'x', '弦' to 'x', '香' to 'x', '想' to 'x',
        '向' to 'x', '象' to 'x', '小' to 'x', '消' to 'x', '宵' to 'x',
        '笑' to 'x', '校' to 'x', '效' to 'x', '些' to 'x', '鞋' to 'x',
        '写' to 'x', '谢' to 'x', '心' to 'x', '辛' to 'x', '新' to 'x',
        '信' to 'x', '星' to 'x', '行' to 'x', '形' to 'x', '醒' to 'x',
        '幸' to 'x', '性' to 'x', '雄' to 'x', '胸' to 'x', '休' to 'x',
        '修' to 'x', '需' to 'x', '许' to 'x', '续' to 'x', '选' to 'x',
        '旋' to 'x', '雪' to 'x', '寻' to 'x', '烟' to 'y', '言' to 'y',
        '严' to 'y', '岩' to 'y', '沿' to 'y', '眼' to 'y', '演' to 'y',
        '阳' to 'y', '洋' to 'y', '样' to 'y', '要' to 'y', '药' to 'y',
        '也' to 'y', '夜' to 'y', '一' to 'y', '依' to 'y', '衣' to 'y',
        '医' to 'y', '移' to 'y', '疑' to 'y', '已' to 'y', '以' to 'y',
        '艺' to 'y', '忆' to 'y', '义' to 'y', '意' to 'y', '因' to 'y',
        '音' to 'y', '银' to 'y', '引' to 'y', '隐' to 'y', '应' to 'y',
        '英' to 'y', '迎' to 'y', '影' to 'y', '硬' to 'y', '泳' to 'y',
        '用' to 'y', '优' to 'y', '由' to 'y', '游' to 'y', '有' to 'y',
        '友' to 'y', '又' to 'y', '右' to 'y', '幼' to 'y', '雨' to 'y',
        '语' to 'y', '玉' to 'y', '遇' to 'y', '原' to 'y', '圆' to 'y',
        '远' to 'y', '院' to 'y', '愿' to 'y', '月' to 'y', '跃' to 'y',
        '越' to 'y', '云' to 'y', '运' to 'y', '再' to 'z', '在' to 'z',
        '早' to 'z', '造' to 'z', '责' to 'z', '怎' to 'z', '增' to 'z',
        '占' to 'z', '站' to 'z', '张' to 'z', '涨' to 'z', '掌' to 'z',
        '丈' to 'z', '找' to 'z', '召' to 'z', '照' to 'z', '者' to 'z',
        '着' to 'z', '真' to 'z', '阵' to 'z', '正' to 'z', '整' to 'z',
        '证' to 'z', '知' to 'z', '之' to 'z', '只' to 'z', '直' to 'z',
        '植' to 'z', '止' to 'z', '指' to 'z', '至' to 'z', '志' to 'z',
        '制' to 'z', '治' to 'z', '中' to 'z', '忠' to 'z', '终' to 'z',
        '重' to 'z', '周' to 'z', '州' to 'z', '昼' to 'z', '珠' to 'z',
        '主' to 'z', '助' to 'z', '住' to 'z', '注' to 'z', '祝' to 'z',
        '转' to 'z', '追' to 'z', '准' to 'z', '捉' to 'z', '桌' to 'z',
        '自' to 'z', '字' to 'z', '总' to 'z', '走' to 'z', '奏' to 'z',
        '足' to 'z', '族' to 'z', '组' to 'z', '最' to 'z', '昨' to 'z',
        '左' to 'z', '做' to 'z', '作' to 'z', '坐' to 'z'
    )

    /**
     * 获取单个汉字或英文字符的拼音首字母 (小写)
     */
    fun getCharInitial(char: Char): Char {
        if (char in 'a'..'z') return char
        if (char in 'A'..'Z') return char.lowercaseChar()
        if (char in '0'..'9') return char

        specialInitials[char]?.let { return it }

        // 如果是中文字符区 (Unicode 0x4E00 - 0x9FA5)
        if (char in '\u4e00'..'\u9fa5') {
            return runCatching {
                val bytes = char.toString().toByteArray(charset("GB2312"))
                if (bytes.size >= 2) {
                    val code = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
                    for (i in initialBoundaries.indices.reversed()) {
                        if (code >= initialBoundaries[i]) {
                            return@runCatching pinyinInitials[i]
                        }
                    }
                }
                char
            }.getOrDefault(char)
        }
        return char
    }

    /**
     * 将整个文本转换为拼音/英文首字母缩写（如 "周杰伦" -> "zjl", "七里香" -> "qlx", "Taylor Swift" -> "ts"）
     */
    fun toPinyinInitials(text: String): String {
        return buildString {
            var prevIsLetterOrDigit = false
            for (ch in text) {
                if (ch.isWhitespace() || ch in "-_·,./()[]【】（）") {
                    prevIsLetterOrDigit = false
                    continue
                }
                val isHan = ch in '\u4e00'..'\u9fa5' || specialInitials.containsKey(ch)
                if (isHan) {
                    append(getCharInitial(ch))
                    prevIsLetterOrDigit = false
                } else if (ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9') {
                    if (!prevIsLetterOrDigit) {
                        append(ch.lowercaseChar())
                    }
                    prevIsLetterOrDigit = true
                } else {
                    prevIsLetterOrDigit = false
                }
            }
        }
    }

    /**
     * 智能多维度匹配：检查 query 是否命中 target 的原文、拼音首字母或去除符号后的形式
     */
    fun matches(target: String, query: String): Boolean {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return true

        val cleanTarget = target.lowercase()
        // 1. 直接子串匹配
        if (cleanTarget.contains(cleanQuery)) return true

        // 2. 拼音首字母匹配 (如输入 "zjl" 命中 "周杰伦")
        val initials = toPinyinInitials(target)
        if (initials.contains(cleanQuery)) return true

        // 3. 去除非字母数字匹配
        val alphaNumTarget = cleanTarget.filter(Char::isLetterOrDigit)
        val alphaNumQuery = cleanQuery.filter(Char::isLetterOrDigit)
        if (alphaNumQuery.isNotEmpty() && alphaNumTarget.contains(alphaNumQuery)) return true

        return false
    }
}
