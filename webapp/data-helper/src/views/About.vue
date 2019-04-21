<template>
    <div class="about">
        <h1>This is an about page</h1>
    </div>
</template>

<style>
    .about {
        text-align: center;
    }
</style>

<script>
    export default {
        data() {
            return {};
        },
        created() {
            this.$store.commit('handleSelect', '/about');
        }
    }
</script>