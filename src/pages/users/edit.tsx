import { useForm, Edit } from "@refinedev/antd";
import { Form, Input, Select, Switch } from "antd";

export const UserEdit = () => {
    const { formProps, saveButtonProps } = useForm({
        resource: "users",
        action: "edit",
    });

    return (
        <Edit saveButtonProps={saveButtonProps} title="Edit User">
            <Form {...formProps} layout="vertical">
                <Form.Item label="Username" name="username">
                    <Input />
                </Form.Item>
                <Form.Item
                    label="Email"
                    name="email"
                    rules={[{ type: "email", message: "Invalid email" }]}
                >
                    <Input />
                </Form.Item>
                <Form.Item
                    label="New Password"
                    name="password"
                    help="Leave blank to keep current password"
                >
                    <Input.Password />
                </Form.Item>
                <Form.Item label="Role" name="role">
                    <Select
                        options={[
                            { label: "User", value: "USER" },
                            { label: "Admin", value: "ADMIN" },
                        ]}
                    />
                </Form.Item>
                <Form.Item label="Blocked" name="blocked" valuePropName="checked">
                    <Switch />
                </Form.Item>
            </Form>
        </Edit>
    );
};
