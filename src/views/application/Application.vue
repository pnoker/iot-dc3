<template>
    <iframe
            class="things-application-iframe"
            frameborder="no"
            border="0"
            marginwidth="0"
            marginheight="0"
            :src="iframeUrl"></iframe>
</template>
<script>
    import {decode} from 'js-base64'

    export default {
        props: {
            url: {
                type: String,
                default: 'http://zhb_nyx.gitee.io/data-view'
            }
        },
        computed: {
            iframeUrl() {
                this.url = decode(this.$route.query.id);
                return this.url;
            }
        }
    }

</script>

<style lang="scss">
    .things-application-iframe {
        width: 100%;
        height: 100%;
    }
</style>
