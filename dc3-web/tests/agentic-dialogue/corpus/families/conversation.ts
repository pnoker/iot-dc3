/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import {family, single, turns} from '../builders';
import type {CaseExpectations, DialogueCase, ScenarioKey} from '../types';

/**
 * Conversational-behavior families: greetings, small talk, identity, meta
 * questions, multi-turn context handling, corrections, ambiguity, emotion, and
 * interruption. Utterances in strict families deliberately avoid the mock
 * engine's routing keywords so the fallback route is deterministic.
 */

const fallback = (live: CaseExpectations['live'], language: CaseExpectations['language'] = 'zh'): CaseExpectations => ({
  scenario: 'dashboard',
  scenarioAccept: ['dashboard'],
  strictScenario: true,
  language,
  live,
});

const tolerant = (scenario: ScenarioKey, live: CaseExpectations['live']): CaseExpectations => ({
  scenario,
  scenarioAccept: ['temperature', 'energy', 'driver', 'point', 'dashboard'],
  language: 'zh',
  live,
});

// ── greetings ───────────────────────────────────────────────────────────────

const greetingUtterances = [
  '你好',
  '您好',
  '哈喽',
  '嗨',
  'hello',
  'hi',
  'hey there',
  'good morning',
  '早上好',
  '中午好',
  '下午好',
  '晚上好',
  '在吗',
  '在不在',
  '有人吗',
  '我来了',
  '我回来了',
  '早',
  '早啊',
  'yo',
  'morning',
  'good evening',
  'howdy',
  'hello there',
  'hi there',
  '你好呀',
  '你好啊，忙不忙',
  'hello, anyone there?',
  '嗨，我上线了',
  'hi, I am back',
  '你好，在线吗',
  'hello，方便吗',
  '哈喽哈喽',
  '嘿！',
  '你好！！！',
  '早安',
  '晚安',
  '午安',
  'hello hello',
  'hey',
  '你好你好',
  '嗨嗨',
  '在么',
  '来了来了',
  'good afternoon',
  'nice to meet you',
  '很高兴见到你',
  '初次见面',
  'hello!',
  '你好。',
];

// ── small talk ──────────────────────────────────────────────────────────────

const smalltalkUtterances = [
  '今天天气真不错',
  '你吃饭了吗',
  '好累啊',
  '周末去哪玩好',
  '最近好忙',
  'coffee or tea',
  'what a day',
  '今天周一，不想上班',
  '哈哈',
  '有点无聊',
  '你也会累吗',
  '下班了，开心',
  '外面在下雨',
  '晚上吃什么好',
  'the weather is nice today',
  'I am so tired',
  'any fun plans for the weekend',
  'busy week huh',
  'lol',
  'kind of bored',
  'do you ever get tired',
  'happy to be off work',
  'it is raining outside',
  'what should I have for dinner',
  '今天心情不错',
  '有点饿了',
  '加油',
  '辛苦了',
  'you got this',
  'hang in there',
  '哈哈哈哈哈',
  '随便聊聊',
  '陪我唠两句',
  '说点开心的',
  'tell me something fun',
  'cheer me up',
  '最近有什么好看的电影',
  '周末睡了个懒觉',
  '地铁今天好挤',
  '午饭吃撑了',
];

// ── identity & capability ───────────────────────────────────────────────────

const capabilityUtterances = [
  '你是谁',
  '你叫什么名字',
  '介绍一下你自己',
  '你是机器人吗',
  '你是 AI 吗',
  '你是真人吗',
  '你会做什么',
  '你能帮我干什么',
  '你的功能有哪些',
  '你会什么技能',
  'who are you',
  'what is your name',
  'introduce yourself',
  'are you a bot',
  'are you AI',
  'are you a real person',
  'what can you do',
  'how can you help me',
  'what are your features',
  'what are you good at',
  '你和别的助手有什么区别',
  '谁创造了你',
  '你基于什么模型',
  '你会学习吗',
  '你有感情吗',
  'how are you different from other assistants',
  'who made you',
  'what model are you based on',
  'do you learn',
  'do you have feelings',
  '你能控制设备吗',
  '你能导出报表吗',
  '你能帮我写文档吗',
  '你能访问数据库吗',
  '你会做协议配置吗',
  'can you control devices',
  'can you export reports',
  'can you write documents for me',
  'can you access the database',
  'can you set up protocol connections',
  '你能听懂我说话吗',
  '你支持哪些语言',
  'do you understand me',
  'which languages do you support',
  '你的数据从哪来',
  'where does your data come from',
  '你记住我说的话吗',
  'do you remember what I say',
];

// ── meta follow-up ──────────────────────────────────────────────────────────

const metaUtterances = [
  '你刚才查了什么',
  '为什么这么回答',
  '你是怎么得出这个结论的',
  '依据是什么',
  '你刚才用了什么工具',
  '数据是从哪查的',
  '能再解释一下吗',
  '换个说法再讲一遍',
  '你说的详细一点',
  '总结一下刚才的内容',
  'what did you just query',
  'why did you answer that',
  'how did you reach that conclusion',
  'what is the evidence',
  'which tool did you use',
  'where did the data come from',
  'can you explain that again',
  'rephrase that for me',
  'be more specific please',
  'summarize what you just said',
  '能不能把结论放开头',
  '太长了，简短点',
  '有出处吗',
  '给我引用来源',
  'make it shorter',
  'put the conclusion first',
  'show your sources',
  'cite your sources',
  '刚才那句没看懂',
  '解释一下这个术语',
  'what does that term mean',
  'I did not catch that',
  '再说一遍重点',
  'repeat the key takeaways',
  '你确定吗',
  'are you sure',
  '有没有可能你搞错了',
  'could you be wrong',
  '帮我翻译成英文',
  'translate that into English',
];

// ── multi-turn context scripts ──────────────────────────────────────────────

const drilldownScripts = [
  turns('3号风机轴承温度怎么样', '那就 3 号风机的呢', '那 2 号呢'),
  turns('帮我看看温度情况', '最高那台是哪台', '它的趋势呢'),
  turns('查一下能耗', '昨天呢', '上周呢'),
  turns('设备在线情况怎么样', '离线的都有谁', '它们什么时候掉的'),
  turns('驱动负载高吗', '最高的那个', '它需要扩容吗'),
  turns('最近告警多吗', '最严重的是哪条', '处理了没有'),
  turns('看看实时值', '湿度呢', '湿度正常吗'),
  turns('温度分析一下', '换个时间段', '改成看上个月'),
  turns('analyze the temperature for me', 'what about fan 2', 'and last week'),
  turns('how is energy usage', 'yesterday only', 'compared to the previous week'),
  turns('any alarms lately', 'the worst one', 'is it resolved'),
  turns('show me the live values', 'what about humidity', 'is that normal'),
  turns('风机温度有点高', '再往前看一周', '有没有规律'),
  turns('设备状态汇总', '重点看告警那台', '它还能运行吗'),
  turns('能耗帮我分析下', '哪个车间贡献最大', '那个车间有什么设备'),
  turns('看看今天的产量', '和昨天比呢', '原因是什么'),
  turns('设备故障率怎么样', '哪类故障最多', '怎么根治'),
  turns('采集成功率多少', '哪个链路最差', '改进方案呢'),
  turns('帮我盘一下家底', '设备有多少', '驱动有多少'),
  turns('最近一周趋势如何', '有没有拐点', '拐点前后发生了什么'),
  turns('看看质量数据', '合格率多少', '不合格集中在哪个班'),
  turns('运维工单多吗', '处理及时吗', '瓶颈在哪'),
];

const pronounScripts = [
  turns('注塑机模具温度怎么样', '它现在正常吗', '那它的历史呢'),
  turns('1 号压缩机温度偏高', '它会影响产量吗', '需要停机吗'),
  turns('S7 驱动负载最高', '它会挂掉吗', '怎么缓解'),
  turns('哪台设备最让人不放心', '它是什么问题', '严重吗'),
  turns('这条告警什么意思', '它多久了', '怎么处理'),
  turns('湿度有点低', '它会影响产品质量吗', '调多少合适'),
  turns('compressor 1 is hot', 'will it fail', 'should we stop it'),
  turns('which device worries you most', 'what is wrong with it', 'is it serious'),
  turns('what does this alarm mean', 'how old is it', 'how do I fix it'),
  turns('湿度波动大吗', '它会一直这样吗', '要不要加个报警'),
  turns('厂房环境怎么样', '它对设备有影响吗', '需要干预吗'),
  turns('哪条产线最稳定', '它的维护记录呢', '下次保养什么时候'),
  turns('夜班表现怎么样', '它比白班差在哪', '怎么改进'),
  turns('这批数据可信吗', '它的来源是哪', '能追溯吗'),
  turns('告警风暴的原因', '它波及了多少设备', '怎么预防'),
  turns('空压站效率如何', '它费电吗', '怎么优化'),
  turns('冷库温度稳定吗', '它波动的原因是什么', '要紧吗'),
];

const correctionScripts = [
  turns('查一下 1 号风机温度', '不对，是 2 号风机', '对，就是它'),
  turns('看下昨天的能耗', '我说错了，是上周', '对，上周'),
  turns('分析车间 A 的设备', '搞错了，是车间 B', '嗯，车间 B'),
  turns('查 3 月的数据', '不对，我说的是 4 月', '好的就 4 月'),
  turns('look at fan 1 temperature', 'sorry, I meant fan 2', 'yes, fan 2'),
  turns('yesterday’s energy', 'my mistake, last week', 'right, last week'),
  turns('analyze workshop A', 'no wait, workshop B', 'yes workshop B'),
  turns('pull March data', 'actually April', 'April it is'),
  turns('温度看 1 号线的', '口误，2 号线', '确认，2 号线'),
  turns('能耗按天统计', '不，按周', '按周来'),
];

const topicSwitchScripts = [
  turns('3 号风机温度怎么样', '对了，顺便问下能耗', '还有驱动情况呢'),
  turns('帮我看看告警', '先不说这个，电费怎么降', '嗯回到告警'),
  turns('设备在线吗', '突然想到，驱动升级了吗', '哦没事了'),
  turns('看下湿度', '算了看温度', '还是湿度吧'),
  turns('temperature looks high', 'by the way, what about energy', 'and drivers'),
  turns('look at the alarms', 'never mind, how do I cut the power bill', 'ok back to alarms'),
  turns('is the device online', 'random thought, did the driver upgrade', 'never mind'),
  turns('check humidity', 'actually temperature', 'humidity again'),
  turns('先看整体情况', '然后聚焦到温度', '再看下能耗'),
  turns('查完设备查告警', '查完告警查能耗', '最后总结一下'),
  turns('先聊设备健康', '再聊能耗优化', '最后聊告警治理'),
  turns('帮我看看接入情况', '顺带看看采集质量', '再看看存储状况'),
  turns('从总览开始', '钻取到车间', '再钻取到产线'),
  turns('讲讲数据链路', '再讲讲告警链路', '最后讲讲命令链路'),
  turns('今天先看在线率', '明天看能耗', '后天看告警趋势'),
  turns('打开设备页', '再打开驱动页', '最后回到总览'),
  turns('帮我梳理上中下游', '重点看上游采集', '下游展示先不管'),
];

const ambiguousScripts = [
  turns('查一下', '就设备那块', '对'),
  turns('看看数据', '最近的', '好'),
  turns('然后呢', '继续说', '好'),
  turns('帮我分析分析', '分析平台情况', '行'),
  turns('有啥问题没', '设备方面', '嗯'),
  turns('check please', 'the device side', 'ok'),
  turns('look at the data', 'recent one', 'sure'),
  turns('and then', 'go on', 'ok'),
  turns('analyze something', 'the platform', 'fine'),
  turns('any issues', 'with devices', 'yes'),
  turns('弄一下那个', '就刚才那个', '对对'),
  turns('搞定了吗', '我说的那个事', '哦好'),
  turns('那个怎么样了', '就是那个呀', '好吧'),
  turns('how is it', 'the thing we discussed', 'right'),
  turns('is it done', 'the thing from earlier', 'ok'),
  turns('弄好了喊我', '好了吗', '收到了'),
  turns('继续', '就按刚才说的', '好'),
  turns('看下结果', '就是刚才跑的那个', '嗯'),
  turns('都正常吧', '就全部那些', '行'),
  turns('差不多了吧', '还差啥', '好嘞'),
  turns('next', 'the next one', 'thanks'),
  turns('anything else', 'the remaining stuff', 'ok'),
];

const emotionalScripts = [
  turns('谢谢你', '不客气', '真的很感谢'),
  turns('太感谢了，帮了大忙', '应该的', '你真棒'),
  turns('不好意思又来麻烦你', '没事', '谢谢'),
  turns('我快急死了，设备报警了', '好的我看看', '拜托了'),
  turns('这个报错我搞不定，很烦', '别急，一步步来', '好谢谢'),
  turns('thanks a lot', 'you are welcome', 'really appreciate it'),
  turns('sorry to bother you again', 'no problem', 'thanks'),
  turns('I am panicking, the alarm is blaring', 'ok let me look', 'please'),
  turns('this error is driving me crazy', 'take it slow', 'thanks'),
  turns('太棒了', '谢谢夸奖', '继续加油'),
  turns('做得好', '谢谢', '保持'),
  turns('有点失望，答案不对', '抱歉，我重新查', '麻烦了'),
  turns('you saved my day', 'glad to help', 'appreciate it'),
  turns('不满意，重来', '好的换个思路', '行吧'),
  turns('完美', '谢谢', '就这些'),
];

const interruptionScripts = [
  turns('帮我详细分析下温度', '停', '不用说了'),
  turns('给我讲讲驱动架构', '别说了', '我知道了'),
  turns('说说告警处理流程', '好了好了', '够了'),
  turns('explain the driver architecture in depth', 'stop', 'that is enough'),
  turns('walk me through alarm handling', 'okay okay', 'enough'),
  turns('详细展开讲讲', '跳过', '下一个'),
  turns('先别说', '我想想', '好了继续'),
  turns('等一下', '我想改一下需求', '现在可以了'),
  turns('hold on', 'let me rethink', 'ok continue'),
  turns('wait', 'change of plan', 'go ahead now'),
  turns('讲快点', '太慢了', '就这样吧'),
  turns('能不能快点', '嗯继续', '行了'),
  turns('重来一遍', '算了不用了', '谢谢'),
  turns('换个话题', '刚才的不要了', '好'),
  turns('stop it', 'never mind', 'thanks'),
];

/**
 * Build every conversational-behavior family.
 * @returns the combined conversation cases
 */
export const conversationCases = (): DialogueCase[] => [
  ...single(
    {
      family: 'conv-greeting',
      subfamily: 'greeting',
      language: 'mixed',
      expectations: fallback({
        keywords: [],
        safety: 'answer',
        toolUse: 'absent',
        judgeFocus: 'A friendly greeting should get a short, warm, onboarding-oriented reply — not a data dump.',
      }),
    },
    greetingUtterances
  ),
  ...single(
    {
      family: 'conv-smalltalk',
      subfamily: 'small-talk',
      language: 'mixed',
      expectations: fallback({
        keywords: [],
        safety: 'redirect',
        toolUse: 'absent',
        judgeFocus: 'Small talk should get a brief friendly reply that gently steers back to platform tasks.',
      }),
    },
    smalltalkUtterances
  ),
  ...single(
    {
      family: 'conv-capability',
      subfamily: 'identity-capability',
      language: 'mixed',
      expectations: fallback({
        keywords: ['助手', 'assistant', '平台', 'platform', '设备', 'device'],
        safety: 'answer',
        toolUse: 'absent',
        judgeFocus: 'Identity/capability questions should describe the IoT assistant scope (device/point/alarm/energy analysis) and its limits.',
      }),
    },
    capabilityUtterances
  ),
  ...single(
    {
      family: 'conv-meta',
      subfamily: 'meta-followup',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'allowed',
        judgeFocus: 'Meta questions should explain reasoning, tools used, or restate concisely as asked.',
      }),
    },
    metaUtterances
  ),
  ...family(
    {
      family: 'conv-multiturn-drilldown',
      subfamily: 'multi-turn',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'allowed',
        judgeFocus: 'Follow-ups must be answered in the context established by earlier turns.',
      }),
    },
    drilldownScripts
  ),
  ...family(
    {
      family: 'conv-multiturn-pronoun',
      subfamily: 'multi-turn',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'allowed',
        judgeFocus: 'Pronoun/anaphora follow-ups ("它呢") must resolve to the entity from earlier turns.',
      }),
    },
    pronounScripts
  ),
  ...family(
    {
      family: 'conv-multiturn-correction',
      subfamily: 'multi-turn',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'allowed',
        judgeFocus: 'Corrections mid-conversation must be honored over the earlier wrong parameter.',
      }),
    },
    correctionScripts
  ),
  ...family(
    {
      family: 'conv-multiturn-switch',
      subfamily: 'multi-turn',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'allowed',
        judgeFocus: 'Topic switches must not lose the earlier thread when asked to return to it.',
      }),
    },
    topicSwitchScripts
  ),
  ...family(
    {
      family: 'conv-multiturn-ambiguous',
      subfamily: 'multi-turn',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'clarify',
        toolUse: 'allowed',
        judgeFocus: 'Ambiguous multi-turn asks should narrow down with clarifying questions.',
      }),
    },
    ambiguousScripts
  ),
  ...family(
    {
      family: 'conv-emotional',
      subfamily: 'emotion-politeness',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'absent',
        judgeFocus: 'Emotional turns need empathetic tone; urgency should trigger a calm, prioritized response.',
      }),
    },
    emotionalScripts
  ),
  ...family(
    {
      family: 'conv-interruption',
      subfamily: 'interruption-cancel',
      language: 'mixed',
      expectations: tolerant('dashboard', {
        keywords: [],
        safety: 'answer',
        toolUse: 'absent',
        judgeFocus: 'Interruptions/cancellations must stop the current line of answer gracefully.',
      }),
    },
    interruptionScripts
  ),
];
