<template>
    <div class="home">
        <img alt="Vue logo" src="../assets/logo.png">
        <HelloWorld msg="Welcome to Your Vue.js App"/>
    </div>
</template>

<style>
    .home {
        margin-top: 61px;
    }
</style>

<script>
    // @ is an alias to /src
    import HelloWorld from '@/components/HelloWorld.vue'

    export default {
        name: 'home',
        data() {
            return {};
        },
        created() {
            this.$store.state.activeIndex = '/'
        },
        components: {
            HelloWorld
        }
    }
</script>
