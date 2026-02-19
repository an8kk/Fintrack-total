import { useParams, useNavigate } from "react-router";
import { useState, useEffect } from "react";
import {
    Card,
    Form,
    Input,
    InputNumber,
    Select,
    Button,
    Typography,
    message,
    Spin,
} from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { API_URL, TOKEN_KEY } from "../../providers/constants";

const { Title } = Typography;

interface CategoryOption {
    id: number;
    name: string;
    type: string;
}

export const TransactionCreate = () => {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [categories, setCategories] = useState<CategoryOption[]>([]);
    const [loadingCats, setLoadingCats] = useState(true);
    const [txType, setTxType] = useState<string | null>(null);

    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const res = await fetch(
                    `${API_URL}/admin/users/${userId}/categories`,
                    {
                        headers: {
                            Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
                        },
                    }
                );
                if (res.ok) {
                    const data = await res.json();
                    setCategories(data);
                }
            } catch {
                // ignore — will show empty or allow manual input
            } finally {
                setLoadingCats(false);
            }
        };
        fetchCategories();
    }, [userId]);

    // Filter categories by selected transaction type
    const filteredCategories = txType
        ? categories.filter(
            (c) => c.type.toUpperCase() === txType.toUpperCase()
        )
        : categories;

    const onFinish = async (values: any) => {
        setLoading(true);
        try {
            const response = await fetch(
                `${API_URL}/admin/users/${userId}/transactions`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
                    },
                    body: JSON.stringify(values),
                }
            );

            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || "Failed to create transaction");
            }

            message.success("Transaction created successfully");
            navigate(`/users/${userId}/transactions`);
        } catch (err: any) {
            message.error(err.message || "Error creating transaction");
        } finally {
            setLoading(false);
        }
    };

    if (loadingCats) {
        return (
            <div style={{ display: "flex", justifyContent: "center", padding: 100 }}>
                <Spin size="large" />
            </div>
        );
    }

    return (
        <div style={{ padding: 24 }}>
            <Button
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate(`/users/${userId}/transactions`)}
                style={{ marginBottom: 16 }}
            >
                Back to Transactions
            </Button>

            <Card style={{ maxWidth: 600, borderRadius: 12 }}>
                <Title level={4}>Create Transaction — User #{userId}</Title>
                <Form layout="vertical" onFinish={onFinish}>
                    <Form.Item
                        label="Type"
                        name="type"
                        rules={[{ required: true, message: "Type is required" }]}
                    >
                        <Select
                            placeholder="Select type"
                            onChange={(val) => setTxType(val)}
                            options={[
                                { label: "Income", value: "INCOME" },
                                { label: "Expense", value: "EXPENSE" },
                            ]}
                        />
                    </Form.Item>
                    <Form.Item
                        label="Amount"
                        name="amount"
                        rules={[{ required: true, message: "Amount is required" }]}
                    >
                        <InputNumber
                            style={{ width: "100%" }}
                            min={0.01}
                            precision={2}
                            placeholder="0.00"
                        />
                    </Form.Item>
                    <Form.Item
                        label="Category"
                        name="category"
                        rules={[{ required: true, message: "Category is required" }]}
                    >
                        {filteredCategories.length > 0 ? (
                            <Select
                                placeholder="Select a category"
                                showSearch
                                optionFilterProp="label"
                                options={filteredCategories.map((c) => ({
                                    label: c.name,
                                    value: c.name,
                                }))}
                            />
                        ) : (
                            <Input placeholder="e.g. Salary, Food, Transport" />
                        )}
                    </Form.Item>
                    <Form.Item label="Description" name="description">
                        <Input.TextArea rows={3} placeholder="Optional description" />
                    </Form.Item>
                    <Form.Item label="Currency" name="currency" initialValue="USD">
                        <Select
                            options={[
                                { label: "USD", value: "USD" },
                                { label: "EUR", value: "EUR" },
                                { label: "KZT", value: "KZT" },
                                { label: "RUB", value: "RUB" },
                            ]}
                        />
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            Create Transaction
                        </Button>
                    </Form.Item>
                </Form>
            </Card>
        </div>
    );
};
