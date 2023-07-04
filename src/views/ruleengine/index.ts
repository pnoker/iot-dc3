import blankCard from '@/components/card/blank/BlankCard.vue'
import { computed, reactive, ref ,defineComponent } from 'vue'
import ruleengineTool from './tool/RuleengineTool.vue'
import ruleengineCard from './card/RuleengineCard.vue'
import { Order } from '@/config/types';
import { getFlowsList } from '@/api/rule';
export default defineComponent({
    components: {
        blankCard,
        ruleengineTool,
        ruleengineCard
    },
    props: {
        embedded: {
            type: String,
            default: () => {
                return ''
            },
        },
    },
    setup(props) {

        // 定义响应式数据
        const reactiveData = reactive({
            loading: true,
            driverTable: {},
            profileTable: {},
            statusTable: {},
            listData: [] as any[],
            query: {},
            order: false,
            page: {
                total: 0,
                size: 12,
                current: 1,
                orders: [] as Order[],
            },
        })
        
        const hasData = computed(() => {
            return !reactiveData.loading && reactiveData.listData?.length < 1
        })

        const list = () => {
            reactiveData.loading = true
            getFlowsList({
                page: reactiveData.page,
                ...reactiveData.query
            }).then((res) => {
                const data = res.data.data
                reactiveData.page.total = data.total
                reactiveData.listData = data.records
                reactiveData.loading = false
            });
        };

        list()

        return {
            hasData,
            reactiveData
        }
    }
});