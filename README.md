https://youtube.com/shorts/WgEyWFcKgPQ?si=H5IHl8TSze6R2WIl
# Spend Smart - Personal Finance Tracker

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Room](https://img.shields.io/badge/Room-0066CC?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/room)

**Spend Smart** is a comprehensive mobile application designed to help users take control of their personal finances through intuitive expense tracking, budget management, gamification, and insightful analytics.

## About The App

Spend Smart transforms financial management into an engaging experience by combining powerful tracking tools with gamification elements. Users can log transactions, set monthly budgets, capture receipt photos, rate their spending satisfaction, and earn achievement badges as they build better financial habits.

### Key Features

- **Transaction Management** - Log income and expenses with titles, amounts, categories, and dates
- **Receipt Capture** - Take photos or pick from gallery to attach receipts to transactions
- **Satisfaction Rating** - Rate transactions as thumbs up/down for spending reflection
- **Monthly Budget Tracking** - Set spending limits with visual progress indicators
- **Advanced Analytics** - View income vs expense graphs, category breakdown pie charts, and transaction history
- **Gamification System** - Earn badges for transaction milestones and login streaks
  - First Transaction (🥇)
  - 5 Transactions (⭐)
  - 10 Transactions (🔥)
  - 25 Transactions (💎)
  - 50 Transactions (🏆)
  - 3-Day Streak (📅)
  - 7-Day Streak (🗓️)
- **Period Filtering** - Analyze finances by week, month, or previous month
- **Profile Management** - Custom avatar photos and user session persistence
- **Secure Authentication** - User registration and login with validation
- **Custom Categories** - Create personalized expense categories
- **Local Database** - All data stored securely on device using Room database

##  Who Is It For?

### Primary Audience

**Young Professionals (Ages 22-35)**
- Early career individuals learning to manage finances
- People transitioning from student to professional life
- Those looking to build savings and investment habits

**University Students**
- First-time money managers
- Students learning budgeting basics
- Part-time workers tracking irregular income

**Budget-Conscious Individuals**
- People aiming to reduce unnecessary spending
- Individuals saving for specific goals
- Anyone wanting to understand their spending patterns

### Secondary Audience

**Small Business Owners**
- Track business expenses with receipt photos
- Monitor operational costs
- Separate personal and business finances

**Freelancers & Gig Workers**
- Track irregular income streams
- Categorize work-related expenses
- Prepare for tax season with transaction history

**Families**
- Manage household budgets
- Track family expenses with satisfaction ratings
- Plan for large purchases

##  Design Decisions

### Color Palette

| Color | Hex Code | Usage |
|-------|----------|-------|
| Deep Navy | `#15174D` | Primary background, toolbars, active states |
| Cream | `#EEEEDD` | Main app background |
| Blue Teal | `#216999` | Secondary text, accents |
| Dark Navy | `#020035` | Cards, elevated surfaces |
| Orange Accent | `#ED4B00` | Primary CTA buttons, expense highlighting |
| Cool Gray | `#BCC8CC` | Secondary text, dividers |
| Off White | `#F4F1EC` | Card backgrounds, surfaces |

**Color Psychology:**
- **Navy tones** convey trust, stability, and professionalism - essential for a financial app
- **Orange accent** creates urgency and draws attention to important actions
- **Green/Red indicators** provide immediate visual feedback for income vs expenses
- **Cream background** reduces eye strain compared to pure white

### Typography

- **Headlines**: Bold, 20-40sp - Creates clear visual hierarchy
- **Body Text**: Regular, 13-16sp - Optimized for readability on mobile
- **Financial Figures**: Bold, large sizes - Emphasizes important monetary values

### Navigation Architecture

**Bottom Navigation Bar**
- **Home** - Dashboard with budget progress and quick stats
- **Transactions** - Add and manage income/expenses with photos
- **Analytics** - Visual charts and spending insights
- **Profile** - User settings, badges, and logout

**Decision Rationale:**
- Four items is optimal for thumb-friendly one-handed use
- Persistent navigation allows quick access to main features
- Visual feedback shows current active screen

### Gamification Design

**Badge System**
- **Progressive milestones** encourage continued app usage
- **Login streaks** reward daily engagement
- **Visual feedback** with emojis and toast notifications
- **Locked badges** appear dimmed in profile

**User Engagement**
- Satisfaction ratings promote mindful spending
- Streak tracking builds habit formation
- Achievement popups provide positive reinforcement

### Data Visualization

**Analytics Screen Features**
- **Income vs Expense Bar Graph** - Visual comparison of financial activity
- **Category Pie Chart** - See where money is going at a glance
- **Transaction History List** - Click for detailed view with receipt photos
- **Period Filters** - Analyze weekly, monthly, or previous month

**Home Dashboard**
- **Budget Progress Bar** - Visual indicator with color-coded warnings (90%+ triggers alert)
- **Balance Cards** - Income, expenses, and net totals
- **Transaction Count** - Total entries logged

### Form Design & Validation

**Transaction Entry**
- Required fields with real-time validation
- Date picker for consistent formatting
- Photo attachment support with preview
- Custom category creation
- Duplicate transaction warning system
- Satisfaction rating buttons

**Registration Validation**
- Password strength meter
- Email format verification
- Password confirmation matching
- Duplicate email checking

### Database Architecture

**Room Database Tables**
- `users` - Account information and credentials
- `transactions` - All financial entries with metadata
- `budgets` - Monthly spending limits
- `categories` - Custom and default categories

**Security Features**
- Password hashing for secure storage
- Session management with SharedPreferences
- Local-only storage - no internet required

### Performance Optimizations

- **Threaded operations** - All database queries run on background threads
- **Lazy loading** - Data loads only when needed
- **RecyclerView** - Efficient list rendering for transactions
- **Image compression** - Optimized photo storage

## 🛠️ Technical Stack

| Technology | Purpose |
|------------|---------|
| Kotlin | Primary development language |
| Android Jetpack | Modern Android components |
| Room Database | Local data persistence |
| Material Design 3 | UI components and styling |
| Coroutines & Threads | Background operations |
| Glide | Image loading and caching |
| CameraX / Intent | Photo capture and gallery access |
| FileProvider | Secure file URI handling |

