import { useState } from "react";
import { Card, Form, Input, Button, Typography, message } from "antd";
import { SendOutlined } from "@ant-design/icons";
import { API_URL, TOKEN_KEY } from "../../providers/constants";

const { Title, Text } = Typography;
const { TextArea } = Input;

export const GlobalNotification = () => {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<string | null>(null);

    const onFinish = async (values: { title: string; message: string }) => {
        setLoading(true);
        setResult(null);
        try {
            const res = await fetch(`${API_URL}/admin/notifications/global`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
                },
                body: JSON.stringify(values),
            });
            if (res.ok) {
                const data = await res.json();
                setResult(`Notification sent to ${data.recipientCount} users`);
                message.success("Notification sent!");
            } else {
                message.error("Failed to send notification");
            }
        } catch {
            message.error("Error sending notification");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: 24 }}>
            <Title level={3}>Global Broadcast</Title>
            <Card style={{ maxWidth: 600, borderRadius: 12 }}>
                <Form layout="vertical" onFinish={onFinish}>
                    <Form.Item
                        name="title"
                        label="Title"
                        rules={[{ required: true, message: "Title is required" }]}
                    >
                        <Input placeholder="e.g. Scheduled Maintenance" />
                    </Form.Item>
                    <Form.Item
                        name="message"
                        label="Message"
                        rules={[{ required: true, message: "Message is required" }]}
                    >
                        <TextArea rows={4} placeholder="Enter the notification message for all users..." />
                    </Form.Item>
                    <Form.Item>
                        <Button
                            type="primary"
                            htmlType="submit"
                            loading={loading}
                            icon={<SendOutlined />}
                            block
                        >
                            Send to All Users
                        </Button>
                    </Form.Item>
                </Form>
                {result && (
                    <Text type="success" strong style={{ display: "block", marginTop: 12 }}>
                        ✅ {result}
                    </Text>
                )}
            </Card>
        </div>
    );
};
