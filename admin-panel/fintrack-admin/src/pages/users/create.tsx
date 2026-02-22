import { Create, useForm } from "@refinedev/antd";
import { Form, Input, Select } from "antd";

export const UserCreate = () => {
    const { formProps, saveButtonProps } = useForm({
        resource: "users",
        action: "create",
    });

    return (
        <Create saveButtonProps={saveButtonProps} title="Create User">
            <Form {...formProps} layout="vertical">
                <Form.Item
                    label="Username"
                    name="username"
                    rules={[{ required: true, message: "Username is required" }]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label="Email"
                    name="email"
                    rules={[
                        { required: true, message: "Email is required" },
                        { type: "email", message: "Invalid email" },
                    ]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label="Password"
                    name="password"
                    rules={[{ required: true, message: "Password is required" }]}
                >
                    <Input.Password />
                </Form.Item>
                <Form.Item label="Role" name="role" initialValue="USER">
                    <Select
                        options={[
                            { label: "User", value: "USER" },
                            { label: "Admin", value: "ADMIN" },
                        ]}
                    />
                </Form.Item>
            </Form>
        </Create>
    );
};
