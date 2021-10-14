import particles from "@/components/particles/particles";

export default {
    name: "login",
    components: {particles},
    data() {
        return {
            formData: {
                tenant: "default",
                name: "pnoker",
                password: "dc3dc3dc3"
            },
            formRule: {
                tenant: [
                    {required: true, message: "请输入租户名", trigger: "blur"}
                ],
                name: [
                    {required: true, message: "请输入用户名", trigger: "blur"}
                ],
                password: [
                    {required: true, message: "请输入密码", trigger: "blur"},
                    {min: 1, message: "密码长度最少为6位", trigger: "blur"}
                ]
            },
            passwordType: "password"
        };
    },
    methods: {
        showPassword() {
            this.passwordType === ""
                ? (this.passwordType = "password")
                : (this.passwordType = "");
        },
        handleLogin() {
            this.$refs['formData'].validate(valid => {
                if (valid) {
                    let loading = this.$loading({
                        lock: true,
                        text: '登录中,请稍后。。。',
                        spinner: "el-icon-loading"
                    });
                    this.$store.dispatch("GenerateSalt", this.formData.name)
                        .then((salt) => {
                            this.$store.dispatch("GenerateToken", {salt, ...this.formData})
                                .then(() => {
                                    this.$router.push({path: '/'}).then(() => loading.close());
                                })
                                .catch(() => loading.close());
                        })
                        .catch(() => loading.close());
                }
            });
        }
    }
};
