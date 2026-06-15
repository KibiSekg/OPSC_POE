
# Spend Smart - Personal Finance Tracker

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Room](https://img.shields.io/badge/Room-0066CC?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/jetpack/androidx/releases/room)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)](https://github.com/features/actions)

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

## Who Is It For?

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

## Design Decisions

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

## Technical Stack

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


## Key User Flows

### Adding a Transaction
1. Navigate to Transactions tab
2. Enter title, amount, and select type (Income/Expense)
3. Choose or create a category
4. Pick date using date picker
5. Optionally attach photo receipt
6. Rate satisfaction (thumbs up/down)
7. Save - system checks for duplicate warnings

### Viewing Analytics
1. Navigate to Analytics tab
2. Select period (This Week, This Month, Last Month)
3. View income vs expense bar graph
4. See category breakdown pie chart
5. Scroll through detailed transaction list
6. Tap any transaction for full details with photo

### Earning Badges
- **Automatic unlocking** based on transaction count
- **Login streaks** tracked daily
- **Toast notifications** when badges earned
- **Profile section** shows all unlocked achievements

## Version Control and CI/CD Automation with GitHub 

For the development of our application, GitHub was utilized as our primary central code repository management platform to ensure seamless team collaboration, version history tracking, and automated quality control. 

To enforce rigorous software engineering standards, we established an automated continuous integration pipeline using GitHub Actions. This setup automates repetitive development tasks across distinct lifecycle events: 

### Build and Test Pipeline (android.yml) 

Every time source code is pushed or a pull request is created against core target branches (main, master, develop), an automated runner environment spins up an isolated virtual container running Ubuntu Linux. 

- **Runtime Environment:** The workflow automatically configures JDK 17, ensuring complete environmental alignment with modern Android compile limits. 

- **Quality Gates:** The pipeline executes code analysis linting (./gradlew lintDebug) and automated JUnit local unit tests (./gradlew testDebugUnitTest) to isolate functional anomalies immediately. 

- **Compilation Verification:** If the code compiles successfully, the workflow builds a debug-ready installation binary (./gradlew assembleDebug) and packages it as an accessible deployment artifact. 

### Repository Workflow Automation (greetings.yml & summarize-issues.yml) 

To make our workspace interactive and lightweight, we modularized our issue-tracking board with event-driven triggers: 

- **Contributor Greetings:** A dedicated interaction action triggers on incoming issues or pull requests, responding with contextual feedback to confirm project momentum. 

- **AI-Powered Issue Summarization:** An advanced workflow detects when a new issue tracking ticket is opened, securely channels user data using isolated environment variables to prevent script injection attacks, runs a local inference step, and posts a neat, one-paragraph structural summary back onto the issue tracker thread for easier tracking. 

## Feature Description: Expense Evaluation & Rating System 

The Expense Evaluation subsystem allows users to record qualitative behavioral feedback alongside quantitative transaction records. It introduces accountability and reflective friction into daily personal budget tracking. 

### User Interface and State Mapping 

Within the transaction layout, users are presented with binary feedback controls using custom Material Design Button interfaces: 

- 👍 Satisfied (THUMBS_UP) 
- 👎 Unsatisfied (THUMBS_DOWN) 

When a user taps a rating control, the selection is visually prioritized by updating the component's internal backgroundTintList to distinct status indicators: a green tint (0xFF4CAF50) for approval, or a red tint (0xFFF44336) for disapproval. Unselected elements revert back to their default structural accent values. This configuration maintains state tracking through a unified string indicator (currentRating) inside the running activity instance. 

## Behavioral Lifecycle: Repeated Dislike Interception Mechanism 

To actively modify bad consumer habits, the application includes an automated intercept mechanism when users attempt to record duplicate transactions they previously disliked. 

### The Evaluation Workflow 

When a user attempts to save a transaction by tapping the save button, the validation workflow executes the following processing path: 

1. User taps Save Transaction button
2. System validates all input fields
3. Application queries database for previous transaction with same title
4. If found with THUMBS_DOWN rating, system triggers intercept
5. AlertDialog appears with warning message
6. User chooses Yes (proceed) or Cancel (modify)

### The System Response: Alert Intercept Trigger 

If the backend database finds a previous transaction with the exact same title marked as THUMBS_DOWN, the system pauses execution and returns to the main thread. It pops open an intuitive structural warning interface (AlertDialog): 

**Previous Dislike** 

*Last time you added "[Transaction Title]" you marked it as 'Unsatisfied'.* 

*Do you still want to add it?* 

1. Yes, add it 
2. Cancel 

### Resolution Paths 

- **Override Access (Yes, add it):** If the user chooses to proceed, the system executes the structural persistence pipeline (persistTransaction), records the input details to SQLite via Room, runs the gamification check, saves the image path locally, and resets the interface form. 

- **Habit Intercept (Cancel):** If the user selects cancel, the dialog box safely dismisses. The form fields remain intact so the user can modify their entry, preventing unreflective or impulse expenses. 


[README.docx](https://github.com/user-attachments/files/28968084/README.docx)

[YuoTube Video Link](https://youtube.com/shorts/5AMV8URKXY8?si=qBAIRLYz4v6NVJMY)    

 
