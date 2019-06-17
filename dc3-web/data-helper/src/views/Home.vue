<template>
    <div class="home">
        <img alt="Vue logo" src="../assets/logo.png">
        <HelloWorld msg="Welcome to Your Vue.js App"/>
    </div>
</template>

<style>
    .home {
        text-align: center;
    }
</style>
<script>
    import HelloWorld from '@/components/HelloWorld.vue'

    export default {
        name: 'home',
        data() {
            return {};
        },
        created() {
            this.$store.commit('handleSelect', '/');
        },
        components: {
            HelloWorld
        }
    }
</script>
