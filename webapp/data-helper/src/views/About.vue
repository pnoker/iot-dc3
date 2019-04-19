<template>
    <div class="about">
        <h1>This is an about page</h1>
    </div>
</template>

<style>
    .about {
        margin-top: 61px;
    }
</style>

<script>
    export default {
        data() {
            return {};
        },
        created() {
            this.$store.state.activeIndex = '/about'
        }
    }
</script>