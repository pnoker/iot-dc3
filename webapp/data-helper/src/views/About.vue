<template>
    <div class="about">
        <h1>This is an about page</h1>
    </div>
</template>

<script>
    export default {
        data() {
            return {};
        },
        created() {
            this.$store.commit('changeNavSelect("/about")');
        },
    }
</script>