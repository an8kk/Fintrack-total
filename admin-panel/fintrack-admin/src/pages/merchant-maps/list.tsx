import { useState, useEffect } from "react";
import {
    Card,
    Table,
    Button,
    Input,
    Select,
    Space,
    Tag,
    Typography,
    message,
    Popconfirm,
    Form,
    Modal,
} from "antd";
import { PlusOutlined, DeleteOutlined } from "@ant-design/icons";
import { API_URL, TOKEN_KEY } from "../../providers/constants";

const { Title } = Typography;

interface MerchantMap {
    id: number;
    keyword: string;
    category: string;
    source: string;
    createdAt: string;
}

export const MerchantMapList = () => {
    const [data, setData] = useState<MerchantMap[]>([]);
    const [loading, setLoading] = useState(true);
    const [modalOpen, setModalOpen] = useState(false);
    const [form] = Form.useForm();

    const fetchData = async () => {
        setLoading(true);
        try {
            const res = await fetch(`${API_URL}/admin/merchant-maps`, {
                headers: { Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` },
            });
            if (res.ok) setData(await res.json());
        } catch {
            message.error("Failed to load merchant mappings");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, []);

    const handleCreate = async (values: { keyword: string; category: string }) => {
        try {
            const res = await fetch(`${API_URL}/admin/merchant-maps`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
                },
                body: JSON.stringify(values),
            });
            if (res.ok) {
                message.success("Mapping created");
                setModalOpen(false);
                form.resetFields();
                fetchData();
            } else {
                message.error("Failed to create mapping");
            }
        } catch {
            message.error("Error creating mapping");
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await fetch(`${API_URL}/admin/merchant-maps/${id}`, {
                method: "DELETE",
                headers: { Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` },
            });
            message.success("Mapping deleted");
            fetchData();
        } catch {
            message.error("Error deleting mapping");
        }
    };

    const categories = [
        "Food", "Transport", "Shopping", "Entertainment", "Utilities",
        "Health", "Education", "Salary", "Subscriptions", "Transfers", "Other",
    ];

    return (
        <div style={{ padding: 24 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
                <Title level={3}>Merchant Category Mappings</Title>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
                    Add Mapping
                </Button>
            </div>

            <Card bordered={false} style={{ borderRadius: 12 }}>
                <Table dataSource={data} loading={loading} rowKey="id" size="small">
                    <Table.Column dataIndex="keyword" title="Keyword" />
                    <Table.Column
                        dataIndex="category"
                        title="Category"
                        render={(val: string) => <Tag color="blue">{val}</Tag>}
                    />
                    <Table.Column
                        dataIndex="source"
                        title="Source"
                        render={(val: string) => (
                            <Tag color={val === "AI_LEARNED" ? "purple" : "green"}>{val}</Tag>
                        )}
                        width={120}
                    />
                    <Table.Column
                        dataIndex="createdAt"
                        title="Created"
                        render={(val: string) => val ? new Date(val).toLocaleDateString() : "—"}
                        width={120}
                    />
                    <Table.Column
                        title="Actions"
                        width={80}
                        render={(_: any, record: MerchantMap) => (
                            <Popconfirm title="Delete this mapping?" onConfirm={() => handleDelete(record.id)}>
                                <Button danger icon={<DeleteOutlined />} size="small" />
                            </Popconfirm>
                        )}
                    />
                </Table>
            </Card>

            <Modal
                title="Add Merchant Mapping"
                open={modalOpen}
                onCancel={() => setModalOpen(false)}
                footer={null}
            >
                <Form form={form} layout="vertical" onFinish={handleCreate}>
                    <Form.Item name="keyword" label="Keyword" rules={[{ required: true }]}>
                        <Input placeholder="e.g. uber, netflix, starbucks" />
                    </Form.Item>
                    <Form.Item name="category" label="Category" rules={[{ required: true }]}>
                        <Select placeholder="Select category" options={categories.map(c => ({ label: c, value: c }))} />
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit" block>Create</Button>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};
